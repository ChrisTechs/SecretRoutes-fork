package xyz.yourboykyle.secretroutes.routes.recording;

import com.google.gson.*;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import xyz.yourboykyle.secretroutes.Main;
import xyz.yourboykyle.secretroutes.config.SRMConfig;
import xyz.yourboykyle.secretroutes.routes.utils.RoomDirectionUtils;
import xyz.yourboykyle.secretroutes.routes.utils.RoomRotationUtils;
import xyz.yourboykyle.secretroutes.routes.utils.RotationUtils;
import xyz.yourboykyle.secretroutes.routes.data.SecretCategory;
import xyz.yourboykyle.secretroutes.routes.data.WaypointType;
import xyz.yourboykyle.secretroutes.utils.ChatUtils;
import xyz.yourboykyle.secretroutes.utils.LogUtils;
import xyz.yourboykyle.secretroutes.utils.multistorage.Triple;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class RouteRecorder {
    private static final RouteRecorder INSTANCE = new RouteRecorder();
    public static RouteRecorder get() { return INSTANCE; }

    public boolean recording = false;
    public String recordingMessage = "Recording...";
    public BlockPos previousLocation = null;

    private JsonObject allSecretRoutes = new JsonObject();
    private JsonArray currentSecretRoute = new JsonArray();
    private JsonObject currentStepData = new JsonObject();

    public RouteRecorder() {
        initCurrentStep();
        importRoutes("routes.json");
    }

    private void initCurrentStep() {
        currentStepData = new JsonObject();
        currentStepData.add("locations", new JsonArray());
        currentStepData.add("etherwarps", new JsonArray());
        currentStepData.add("mines", new JsonArray());
        currentStepData.add("interacts", new JsonArray());
        currentStepData.add("tnts", new JsonArray());
        currentStepData.add("enderpearls", new JsonArray());
        currentStepData.add("enderpearlangles", new JsonArray());
    }

    public void startRecording() {
        recording = true;
        currentSecretRoute = new JsonArray();
        initCurrentStep();
        ChatUtils.sendVerboseMessage("§eRecording started...", "Recording");
    }

    public void stopRecording() {
        recording = false;
        finalizeRoute();
        ChatUtils.sendVerboseMessage("§eRecording stopped.", "Recording");

        previousLocation = null;
    }

    public boolean addWaypoint(WaypointType type, BlockPos pos) {
        if (!recording) return false;
        Main.checkRoomData();

        BlockPos relPos = RoomRotationUtils.actualToRelative(pos, RoomDirectionUtils.roomDirection(), RoomDirectionUtils.roomCorner());
        JsonArray posArray = toJsonArray(relPos.getX(), relPos.getY(), relPos.getZ());

        String key = getTypeKey(type);
        if (key == null) return false;

        return addUnique(currentStepData.getAsJsonArray(key), posArray);
    }

    public void addWaypoint(WaypointType type, ClientPlayerEntity player) {
        if (!recording || type != WaypointType.ENDERPEARLS) return;
        Main.checkRoomData();

        Triple<Double, Double, Double> relPos = RoomRotationUtils.actualToRelative(player.getX(), player.getY(), player.getZ(),
                RoomDirectionUtils.roomDirection(), RoomDirectionUtils.roomCorner());

        JsonArray posArray = new JsonArray();
        posArray.add(relPos.getOne());
        posArray.add(relPos.getTwo());
        posArray.add(relPos.getThree());

        if (addUnique(currentStepData.getAsJsonArray("enderpearls"), posArray)) {
            float pitch = player.getPitch();
            float relYaw = RotationUtils.actualToRelativeYaw(player.getYaw() % 360, RoomDirectionUtils.roomDirection());

            JsonArray angleArray = new JsonArray();
            angleArray.add(pitch);
            angleArray.add(relYaw);
            currentStepData.getAsJsonArray("enderpearlangles").add(angleArray);

            setRecordingMessage("Added Ender Pearl waypoint.");
        }
    }

    public boolean addSecret(SecretCategory category, BlockPos pos) {
        if (!recording) return false;
        Main.checkRoomData();

        BlockPos relPos = RoomRotationUtils.actualToRelative(pos, RoomDirectionUtils.roomDirection(), RoomDirectionUtils.roomCorner());
        JsonArray posArray = toJsonArray(relPos.getX(), relPos.getY(), relPos.getZ());

        JsonObject secretObj = new JsonObject();
        secretObj.addProperty("type", category.name().toLowerCase());
        secretObj.add("location", posArray);

        for (JsonElement step : currentSecretRoute) {
            JsonObject stepObj = step.getAsJsonObject();
            if (stepObj.has("secret") && stepObj.getAsJsonObject("secret").get("location").equals(posArray)) {
                return false;
            }
        }

        currentStepData.add("secret", secretObj);
        return true;
    }

    public void newSecret() {
        if (!recording) return;
        currentSecretRoute.add(currentStepData);
        initCurrentStep();
    }

    private void finalizeRoute() {
        if (currentStepData.size() > 0 && !currentStepData.getAsJsonArray("locations").isEmpty()) {
            currentSecretRoute.add(currentStepData);
        }

        int routeNumber = SRMConfig.get().routeNumber;
        String key = RoomDirectionUtils.roomName() + (routeNumber != 0 ? ":" + routeNumber : "");
        allSecretRoutes.add(key, currentSecretRoute);
    }

    public void importRoutes(String fileName) {
        String filePath = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + fileName;
        if (!new File(filePath).exists()) return;

        try (FileReader reader = new FileReader(filePath)) {
            allSecretRoutes = new Gson().fromJson(reader, JsonObject.class);
        } catch (Exception e) {
            LogUtils.error(e);
        }
    }

    public void exportAllRoutes() {
        String filePath = System.getProperty("user.home") + File.separator + "Downloads";
        File file = new File(filePath, "routes.json");

        try (FileWriter writer = new FileWriter(file)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            writer.write(gson.toJson(allSecretRoutes));
            ChatUtils.sendChatMessage(Formatting.DARK_GREEN + "Exported routes to " + file.getAbsolutePath());
        } catch (IOException e) {
            LogUtils.error(e);
        }
    }
    
    private boolean addUnique(JsonArray array, JsonArray element) {
        for (JsonElement e : array) {
            if (e.equals(element)) return false;
        }
        array.add(element);
        return true;
    }

    private JsonArray toJsonArray(Number... numbers) {
        JsonArray arr = new JsonArray();
        for (Number n : numbers) arr.add(new JsonPrimitive(n));
        return arr;
    }

    private String getTypeKey(WaypointType type) {
        return switch (type) {
            case ETHERWARPS -> "etherwarps";
            case MINES -> "mines";
            case INTERACTS -> "interacts";
            case TNTS -> "tnts";
            case LOCATIONS -> "locations";
            default -> null;
        };
    }

    public void setRecordingMessage(String message) {
        this.recordingMessage = message;
        ChatUtils.sendVerboseMessage(message, "Recording");
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                if (this.recordingMessage.equals(message)) {
                    this.recordingMessage = "Recording...";
                }
            } catch (InterruptedException ignored) {}
        }).start();
    }
}