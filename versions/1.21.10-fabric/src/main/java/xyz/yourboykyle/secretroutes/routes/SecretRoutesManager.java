package xyz.yourboykyle.secretroutes.routes;

import net.minecraft.util.math.BlockPos;
import xyz.yourboykyle.secretroutes.events.OnSecretComplete;
import xyz.yourboykyle.secretroutes.routes.data.RouteStep;
import xyz.yourboykyle.secretroutes.routes.data.Secret;
import xyz.yourboykyle.secretroutes.routes.data.SecretCategory;
import xyz.yourboykyle.secretroutes.utils.LogUtils;

import java.util.*;

public class SecretRoutesManager {
    private static final SecretRoutesManager INSTANCE = new SecretRoutesManager();
    public static SecretRoutesManager get() { return INSTANCE; }

    // State
    private String currentRoomName;
    private final List<Secret> allSecretsInRoom = new ArrayList<>();
    private final Set<BlockPos> foundSecrets = new HashSet<>();

    // Route State
    private List<RouteStep> activeRoute = null;
    private int currentStepIndex = 0;

    // Locked Chest State
    private BlockPos currentLeverPos = null;
    private long leverBannerExpiry = 0;

    public void onRoomChange(String roomName) {
        reset();
        this.currentRoomName = roomName;
        if (roomName == null) return;

        allSecretsInRoom.addAll(RouteFileManager.getSecrets(roomName));

        List<RouteStep> route = RouteFileManager.getRoute(roomName);
        if (route != null && !route.isEmpty()) {
            this.activeRoute = route;
            LogUtils.info("Route loaded for " + roomName);
        }
    }

    public void overrideRoute(String roomName, List<RouteStep> newRoute) {
        reset();
        this.currentRoomName = roomName;
        this.activeRoute = newRoute;
        LogUtils.info("Custom route loaded for " + roomName);
    }

    public void reset() {
        allSecretsInRoom.clear();
        foundSecrets.clear();
        activeRoute = null;
        currentStepIndex = 0;
        resetLeverState();
    }

    public void nextSecret() {
        if (activeRoute == null) return;
        OnSecretComplete.onSecretCompleteNoKeybind();
        if (currentStepIndex < activeRoute.size() - 1) {
            currentStepIndex++;
            resetLeverState();
        }
    }

    public void nextSecretKeybind() {
        if (activeRoute != null && currentStepIndex < activeRoute.size() - 1) {
            currentStepIndex++;
            resetLeverState();
        }
    }

    public void previousSecretKeybind() {
        if (activeRoute != null && currentStepIndex > 0) {
            currentStepIndex--;
            resetLeverState();
        }
    }

    public RouteStep getCurrentStep() {
        if (activeRoute == null || currentStepIndex >= activeRoute.size()) return null;
        return activeRoute.get(currentStepIndex);
    }

    public RouteStep getStep(int index) {
        if (activeRoute == null || index >= activeRoute.size() || index < 0) return null;
        return activeRoute.get(index);
    }

    public int getCurrentStepIndex() { return currentStepIndex; }
    public int getTotalSteps() { return activeRoute == null ? 0 : activeRoute.size(); }
    public String getRoomName() { return currentRoomName; }
    public boolean hasRoute() { return activeRoute != null; }

    public void markSecretFound(BlockPos pos) {
        foundSecrets.add(pos);
    }

    public void unmarkSecretFound(BlockPos pos) {
        foundSecrets.remove(pos);
    }

    public boolean isSecretFound(BlockPos pos) {
        return foundSecrets.contains(pos);
    }

    public List<Secret> getAllSecrets() {
        return allSecretsInRoom;
    }

    public void handleLockedChestInteract(BlockPos chestPos) {
        Optional<Secret> chest = allSecretsInRoom.stream()
                .filter(s -> s.pos().equals(chestPos) && s.is(SecretCategory.CHEST))
                .findFirst();

        if (chest.isEmpty()) return;

        String chestName = chest.get().name();
        String id = extractId(chestName);

        Optional<Secret> lever = allSecretsInRoom.stream()
                .filter(s -> s.is(SecretCategory.LEVER) && s.name().contains(id))
                .findFirst();

        if (lever.isPresent()) {
            this.currentLeverPos = lever.get().pos();
            this.leverBannerExpiry = System.currentTimeMillis() + 5000;
        }
    }

    public BlockPos getCurrentLeverPos() { return currentLeverPos; }

    public void resetLeverState() {
        this.currentLeverPos = null;
        this.leverBannerExpiry = 0;
    }

    public boolean shouldShowLeverBanner() {
        return currentLeverPos != null && System.currentTimeMillis() < leverBannerExpiry;
    }

    private String extractId(String name) {
        if (name.contains("/")) return name.split("/")[1];
        if (name.contains(" ")) return name.split(" ")[1];
        return name;
    }
}