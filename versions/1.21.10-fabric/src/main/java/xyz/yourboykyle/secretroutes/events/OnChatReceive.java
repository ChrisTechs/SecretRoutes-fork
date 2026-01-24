package xyz.yourboykyle.secretroutes.events;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import xyz.yourboykyle.secretroutes.config.SRMConfig;
import xyz.yourboykyle.secretroutes.routes.utils.RoomDirectionUtils;
import xyz.yourboykyle.secretroutes.routes.utils.RoomRotationUtils;
import xyz.yourboykyle.secretroutes.routes.SecretRoutesManager;
import xyz.yourboykyle.secretroutes.routes.data.RouteStep;
import xyz.yourboykyle.secretroutes.routes.data.SecretCategory;
import xyz.yourboykyle.secretroutes.utils.ChatUtils;
import xyz.yourboykyle.secretroutes.utils.LocationUtils;
import xyz.yourboykyle.secretroutes.utils.LogUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OnChatReceive {

    private static final String[] BOSS_NAMES = {
            "The Watcher", "Bonzo", "Scarf", "The Professor", "Thorn",
            "Livid", "Sadan", "Maxor", "Storm", "Goldor", "Necron"
    };

    private static boolean allFound = false;

    public static void register() {
        OnChatReceive instance = new OnChatReceive();
        ClientReceiveMessageEvents.ALLOW_GAME.register(instance::handleChatReceive);
    }

    public static boolean isAllFound() {
        return allFound;
    }

    private boolean handleChatReceive(Text message, boolean overlay) {
        if (!LocationUtils.isOnSkyblock()) return true;

        String plainText = Formatting.strip(message.getString()).trim();

        if (overlay) {
            handleSecretsCount(plainText);
            return true;
        }

        if (plainText.equals("That chest is locked")) {
            handleLockedChest();
            return true;
        }

        SRMConfig config = SRMConfig.get();
        if (config.hideBossMessages && plainText.contains("[BOSS]")) {
            if (config.hideWatcher && plainText.contains("The Watcher")) return false;
            if (config.hideBonzo && plainText.contains("Bonzo")) return false;
            if (config.hideScarf && plainText.contains("Scarf")) return false;
            if (config.hideProfessor && plainText.contains("The Professor")) return false;
            if (config.hideThorn && plainText.contains("Thorn")) return false;
            if (config.hideLivid && plainText.contains("Livid")) return false;
            if (config.hideSadan && plainText.contains("Sadan")) return false;
            if (config.hideWitherLords && (plainText.contains("Maxor") || plainText.contains("Storm") || plainText.contains("Goldor") || plainText.contains("Necron"))) return false;

            if (config.bloodNotif && plainText.contains("That will be enough for now")) {
                OnGuiRender.spawnNotifTime = System.currentTimeMillis() + config.bloodBannerDuration;
            }
        }

        return true;
    }

    private void handleSecretsCount(String text) {
        Matcher matcher = Pattern.compile("(?<found>\\d+)/(?<total>\\d+) Secrets").matcher(text);
        if (matcher.find()) {
            int total = Integer.parseInt(matcher.group("total"));
            int found = Integer.parseInt(matcher.group("found"));

            if (total == found) {
                if (!allFound) ChatUtils.sendVerboseMessage("§aAll secrets found!", "Actionbar");
                allFound = true;
            } else {
                ChatUtils.sendVerboseMessage("§9(" + found + "/" + total + ")", "Actionbar");
                allFound = false;
            }
        }
    }

    private void handleLockedChest() {
        LogUtils.info("§aLocked chest detected!");
        SecretRoutesManager manager = SecretRoutesManager.get();

        BlockPos lastInteract = OnPlayerInteract.getLastInteractPos();

        if (lastInteract != null) {
            BlockPos relPos = RoomRotationUtils.actualToRelative(lastInteract,
                    RoomDirectionUtils.roomDirection(), RoomDirectionUtils.roomCorner());

            manager.unmarkSecretFound(relPos);

            manager.handleLockedChestInteract(relPos);
        }

        RouteStep step = manager.getCurrentStep();
        if (step != null && step.targetSecret() != null && step.targetSecret().is(SecretCategory.CHEST)) {
            manager.previousSecretKeybind();
        }
    }

    private boolean handleBossMessages(String plainText) {
        if (SRMConfig.get().bloodNotif && plainText.contains("That will be enough for now")) {
            OnGuiRender.spawnNotifTime = System.currentTimeMillis() + SRMConfig.get().bloodBannerDuration;
        }

        for (String boss : BOSS_NAMES) {
            if (plainText.contains(boss)) return false;
        }
        return true;
    }
}