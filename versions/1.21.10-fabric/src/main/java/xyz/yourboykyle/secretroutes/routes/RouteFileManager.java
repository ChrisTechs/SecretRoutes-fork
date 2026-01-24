package xyz.yourboykyle.secretroutes.routes;

import com.google.gson.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import xyz.yourboykyle.secretroutes.Main;
import xyz.yourboykyle.secretroutes.config.SRMConfig;
import xyz.yourboykyle.secretroutes.routes.data.RouteStep;
import xyz.yourboykyle.secretroutes.routes.data.Secret;
import xyz.yourboykyle.secretroutes.routes.data.SecretCategory;
import xyz.yourboykyle.secretroutes.utils.FileUtils;
import xyz.yourboykyle.secretroutes.utils.LogUtils;
import xyz.yourboykyle.secretroutes.utils.VersionUtils;

import java.io.*;
import java.net.URL;
import java.util.*;

import static xyz.yourboykyle.secretroutes.utils.JsonUtils.*;

public class RouteFileManager {
    private static final String ROUTES_URL = "https://raw.githubusercontent.com/yourboykyle/SecretRoutes/main/routes.json";
    private static final String PEARL_ROUTES_URL = "https://raw.githubusercontent.com/yourboykyle/SecretRoutes/main/pearlroutes.json";

    private static final Map<String, List<Secret>> SECRET_CACHE = new HashMap<>();
    private static final Map<String, List<RouteStep>> ROUTE_CACHE = new HashMap<>();

    public static void init() {
        checkAndDownloadFiles();
        loadInternalSecrets();
        reloadRoutes();
    }

    private static void checkAndDownloadFiles() {
        File routesDir = new File(Main.ROUTES_PATH);
        if (!routesDir.exists()) routesDir.mkdirs();

        checkFile(new File(routesDir, "routes.json"), ROUTES_URL);
        checkFile(new File(routesDir, "pearlroutes.json"), PEARL_ROUTES_URL);
    }

    private static void checkFile(File file, String urlString) {
        boolean needsUpdate = false;
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                if (json.has("Version")) {
                    if (VersionUtils.isLower(json.get("Version").getAsString())) needsUpdate = true;
                } else {
                    needsUpdate = true;
                }
            } catch (Exception e) {
                needsUpdate = true;
            }
        } else {
            needsUpdate = true;
        }

        if (needsUpdate) {
            try {
                LogUtils.info("Downloading " + file.getName());
                FileUtils.downloadFile(file, new URL(urlString));
            } catch (Exception e) {
                LogUtils.error(new IOException("Failed to download " + file.getName(), e));
            }
        }
    }

    private static void loadInternalSecrets() {
        SECRET_CACHE.clear();
        String path = "/assets/" + Main.MODID + "/secretlocations.json";

        try (InputStream stream = RouteFileManager.class.getResourceAsStream(path)) {
            if (stream == null) return;
            try (Reader reader = new InputStreamReader(stream)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                for (String roomKey : root.keySet()) {
                    if (roomKey.equalsIgnoreCase("copyright") || roomKey.equalsIgnoreCase("license")) continue;

                    List<Secret> secrets = new ArrayList<>();
                    for (JsonElement el : root.getAsJsonArray(roomKey)) {
                        JsonObject obj = el.getAsJsonObject();
                        SecretCategory cat = SecretCategory.fromString(
                                obj.has("category") ? obj.get("category").getAsString() : "unknown");

                        secrets.add(new Secret(
                                obj.get("secretName").getAsString(),
                                cat,
                                new BlockPos(obj.get("x").getAsInt(), obj.get("y").getAsInt(), obj.get("z").getAsInt())
                        ));
                    }
                    SECRET_CACHE.put(roomKey.toLowerCase(), secrets);
                }
            }
        } catch (Exception e) {
            LogUtils.error(e);
        }
    }

    public static void reloadRoutes() {
        ROUTE_CACHE.clear();
        String fileName = (SRMConfig.get().routeType == SRMConfig.RouteType.PEARLS) ?
                (SRMConfig.get().pearlRoutesFileName.isEmpty() ? "pearlroutes.json" : SRMConfig.get().pearlRoutesFileName) :
                (SRMConfig.get().routesFileName.isEmpty() ? "routes.json" : SRMConfig.get().routesFileName);

        File file = new File(Main.ROUTES_PATH + File.separator + fileName);
        if (!file.exists()) return;

        try (FileReader reader = new FileReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (String key : root.keySet()) {
                if (key.equalsIgnoreCase("Version") || key.startsWith("#")) continue;
                ROUTE_CACHE.put(key.toLowerCase(), parseRouteSteps(root.getAsJsonArray(key)));
            }
            LogUtils.info("Loaded routes from " + fileName);
        } catch (Exception e) {
            LogUtils.error(e);
        }
    }

    private static List<RouteStep> parseRouteSteps(JsonArray array) {
        List<RouteStep> steps = new ArrayList<>();
        for (JsonElement el : array) {
            JsonObject obj = el.getAsJsonObject();
            Secret targetSecret = null;
            String secretType = "none";

            if (obj.has("secret")) {
                JsonObject sObj = obj.getAsJsonObject("secret");
                secretType = sObj.has("type") ? sObj.get("type").getAsString() : "none";

                BlockPos secretPos = null;
                if (sObj.has("location")) {
                    JsonArray loc = sObj.getAsJsonArray("location");
                    if (loc.size() >= 3) {
                        secretPos = new BlockPos(loc.get(0).getAsInt(), loc.get(1).getAsInt(), loc.get(2).getAsInt());
                    }
                } else if (sObj.has("x") && sObj.has("y") && sObj.has("z")) {
                    secretPos = new BlockPos(sObj.get("x").getAsInt(), sObj.get("y").getAsInt(), sObj.get("z").getAsInt());
                }

                if (secretPos != null) {
                    SecretCategory category = SecretCategory.fromString(secretType);
                    targetSecret = new Secret("Target", category, secretPos);
                }
            }

            steps.add(new RouteStep(
                    toPosList(obj.getAsJsonArray("etherwarps")),
                    toPosList(obj.getAsJsonArray("mines")),
                    toPosList(obj.getAsJsonArray("interacts")),
                    toPosList(obj.getAsJsonArray("tnts")),
                    toVec3dList(obj.getAsJsonArray("enderpearls")),
                    toFloatListList(obj.getAsJsonArray("enderpearlangles")),
                    toPosList(obj.getAsJsonArray("locations")),
                    targetSecret,
                    secretType
            ));
        }
        return steps;
    }

    public static List<Secret> getSecrets(String roomName) {
        return SECRET_CACHE.getOrDefault(roomName.toLowerCase(), Collections.emptyList());
    }
    public static List<RouteStep> getRoute(String roomName) {
        return ROUTE_CACHE.get(roomName.toLowerCase());
    }
}