package xyz.yourboykyle.secretroutes;

/*
 * Secret Routes Mod - Secret Route Waypoints for Hypixel Skyblock Dungeons
 * Copyright 2025 yourboykyle & R-aMcC
 *
 * <DO NOT REMOVE THIS COPYRIGHT NOTICE>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import xyz.yourboykyle.secretroutes.commands.*;
import xyz.yourboykyle.secretroutes.config.SRMConfig;
import xyz.yourboykyle.secretroutes.config.huds.CurrentRoomHUD;
import xyz.yourboykyle.secretroutes.config.huds.RecordingHUD;
import xyz.yourboykyle.secretroutes.rendering.SecretRenderBackend;
import xyz.yourboykyle.secretroutes.routes.RouteFileManager;
import xyz.yourboykyle.secretroutes.events.*;
import xyz.yourboykyle.secretroutes.events.recording.RecordBlockBreak;
import xyz.yourboykyle.secretroutes.events.recording.RecordMouseInput;
import xyz.yourboykyle.secretroutes.routes.SecretRoutesManager;
import xyz.yourboykyle.secretroutes.routes.recording.RouteRecorder;
import xyz.yourboykyle.secretroutes.utils.*;
import xyz.yourboykyle.secretroutes.utils.autoupdate.UpdateManager;
import xyz.yourboykyle.secretroutes.routes.roomdetection.DungeonScanner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

import static xyz.yourboykyle.secretroutes.utils.ChatUtils.sendChatMessage;

public class Main implements ClientModInitializer {
    public static final String MODID = "secretroutesmod"; // Replaced template variable
    public static final String NAME = "Secret Routes Mod";
    public static final String VERSION = "1.0.0";
    public static final Identifier ID = Identifier.of(MODID);
    public static final String CONFIG_FOLDER_PATH = MinecraftClient.getInstance().runDirectory.getAbsolutePath() + File.separator + "config" + File.separator + "SecretRoutes";
    public static final String ROUTES_PATH = MinecraftClient.getInstance().runDirectory.getAbsolutePath() + File.separator + "config" + File.separator + "SecretRoutes" + File.separator + "routes";
    public static final String COLOR_PROFILE_PATH = MinecraftClient.getInstance().runDirectory.getAbsolutePath() + File.separator + "config" + File.separator + "SecretRoutes" + File.separator + "colorprofiles";
    public static final String tmpDir = MinecraftClient.getInstance().runDirectory.getAbsolutePath() + File.separator + "SecretRoutes" + File.separator + "tmp";
    public final static File logDir = new File(MinecraftClient.getInstance().runDirectory.getAbsolutePath() + File.separator + "logs" + File.separator + "SecretRoutes");
    private final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    public static File outputLogs;

    public static UpdateManager updateManager = new UpdateManager();

    public static Main instance;

    // HUD instances
    public static RecordingHUD recordingHUD;
    public static CurrentRoomHUD currentRoomHUD;

    // Keybinds
    public static KeyBinding nextSecretKey;
    public static KeyBinding lastSecretKey;
    public static KeyBinding toggleSecretsKey;

    public static void checkRoomData() {
        // Old method, kept for compatibility
    }

    public static void checkProfilesData() {
        try {
            String filePath = "default.json";

            File colorProfileDir = new File(COLOR_PROFILE_PATH);
            if (!colorProfileDir.exists()) {
                colorProfileDir.mkdirs();
            }
            if (FileUtils.getFileNames(COLOR_PROFILE_PATH).isEmpty()) {
                ConfigUtils.writeColorConfig(filePath);
            }
            //Implement logic for writing to file
        } catch (Exception e) {
            LogUtils.error(e);
        }
    }

    public static void checkPBData() {
        try {
            String filePath = CONFIG_FOLDER_PATH + File.separator + "personal_bests.json";

            File configDir = new File(CONFIG_FOLDER_PATH);
            if (!configDir.exists()) {
                configDir.mkdirs();
            }

            File configFile = new File(filePath);
            if (!configFile.exists()) {
                configFile.createNewFile();
                FileWriter pbWriter = new FileWriter(configFile);
                pbWriter.write("{}");
                pbWriter.close();
            }
        } catch (Exception e) {
            LogUtils.error(e);
        }
    }

    // Unused
    private static void onServerConnect() {
        new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (Exception e) {
                // nothing needed
            }

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.getCurrentServerEntry() == null) return;
            String serverName = mc.getCurrentServerEntry().address.toLowerCase();
            if (serverName.contains("hypixel.") || serverName.contains("fakepixel.") || SRMConfig.get().disableServerChecking) {

                if (SRMConfig.get().autoCheckUpdates) {
                    new Thread(() -> {
                        try {
                            Main.updateManager.checkUpdate(false);
                        } catch (Exception e) {
                            LogUtils.error(e);
                        }
                    }).start();
                }

                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                    } catch (Exception ignored) {
                    }
                    byte res = APIUtils.addMember();
                    if (res == 1) {
                        sendChatMessage("§aFirst logon detected... things work");
                    } else if (res == 0) {
                        sendChatMessage("§aWelcome back!");
                    }
                }).start();

                if (!APIUtils.apiQueued) {
                    Runtime.getRuntime().addShutdownHook(new Thread(APIUtils::offline));
                    APIUtils.apiQueued = true;
                }
            }
        }).start();
    }

    public static void toggleSecretsKeybind() {
        if (SRMConfig.get().modEnabled) {
            SRMConfig.get().modEnabled = false;
            sendChatMessage(Formatting.RED + "Secret Routes Mod secret rendering has been disabled.");
        } else {
            SRMConfig.get().modEnabled = true;
            sendChatMessage(Formatting.GREEN + "Secret Routes Mod secret rendering has been enabled.");
        }
    }

    @Override
    public void onInitializeClient() {
        instance = this;

        LocationUtils.init();

        String jarpath = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        // Set up logging system
        String date = sdf.format(System.currentTimeMillis());
        outputLogs = new File(logDir + File.separator + "LATEST-" + date + ".log");
        if (!logDir.exists()) {
            logDir.mkdirs();
        } else {
            File[] files = logDir.listFiles((dir, name) -> name.startsWith("LATEST"));
            for (File file : files) {
                File[] logFiles = logDir.listFiles((dir, name) -> name.contains(date));
                int logsNo = logFiles == null ? 1 : logFiles.length;
                String newName = file.getName().replaceFirst("LATEST-", "").split("\\.")[0] + "-" + logsNo + ".log";
                File renamedFile = new File(logDir + File.separator + newName);
                file.renameTo(renamedFile);
            }

        }
        try {
            outputLogs.createNewFile();
        } catch (IOException e1) {
            System.out.println("Secret Routes Mod logging file creation failed :(");
            e1.printStackTrace();
        }
        LogUtils.info("Jarpath: " + jarpath);
        if (!new File(tmpDir).exists()) {
            new File(tmpDir).mkdirs();
        }

        File batchFile = new File(tmpDir + File.separator + "update.bat");
        if (batchFile.exists()) {
            batchFile.delete();
        }
        File shellFile = new File(tmpDir + File.separator + "update.sh");
        if (shellFile.exists()) {
            shellFile.delete();
        }
        LogUtils.info("§bSetting ssl certificate");
        SSLUtils.setSSlCertificate();

        ConfigUtils.init();

        // find previous config and rename
        File configFolder = FabricLoader.getInstance().getConfigDir().resolve("SecretRoutes").toFile();
        if (!configFolder.exists()) configFolder.mkdirs();

        File oldFile = new File(configFolder, "xyz.yourboykyle.secretroutes.config.json");
        File newFile = new File(configFolder, "config.json");

        if (oldFile.exists() && !newFile.exists()) {
            if (oldFile.renameTo(newFile)) {
                LogUtils.info("Migrated legacy config file: xyz.yourboykyle.secretroutes.config.json -> config.json");
            } else {
                LogUtils.error(new IOException("Failed to migrate legacy config file!"));
            }
        }

        // Load YACL Config
        SRMConfig.HANDLER.load();

        // Initialize Data Managers
        RouteFileManager.init();
        SecretRenderBackend.register();

        // Initialize HUDs
        recordingHUD = new RecordingHUD();
        currentRoomHUD = new CurrentRoomHUD();

        // Register HUD Rendering
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            recordingHUD.render(drawContext);
            currentRoomHUD.render(drawContext);
        });

        checkProfilesData();
        checkPBData();
        PBUtils.loadPBData();

        // Register Keybinds
        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of(MODID, "SecretRoutes"));
        nextSecretKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Next Secret", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_BRACKET, cat
        ));
        lastSecretKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Last Secret", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, cat
        ));
        toggleSecretsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Toggle Secrets", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_BACKSPACE, cat
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (nextSecretKey.wasPressed()) {
                if (LocationUtils.isInDungeons()) SecretRoutesManager.get().nextSecretKeybind();
            }
            if (lastSecretKey.wasPressed()) {
                if (LocationUtils.isInDungeons()) SecretRoutesManager.get().previousSecretKeybind();
            }
            if (toggleSecretsKey.wasPressed()) {
                toggleSecretsKeybind();
            }
        });

        // Fabric Events
        GuildEvents.register();
        RecordBlockBreak.register();
        OnChatReceive.register();
        OnGuiRender.register();
        OnItemPickedUp.register();
        RecordMouseInput.register();
        OnPlayerInteract.register();
        OnPlayerTick.register();

        // Commands
        ChangeColorProfile.register();
        ChangeRoute.register();
        Debug.register();
        LoadRoute.register();
        Recording.register();
        SRM.register();

        DungeonScanner.init();

        // RouteUtils removed.
        /*if (SRMConfig.get().autoUpdateRoutes) {
            LogUtils.info("Checking for route updates...");
            new Thread(() -> {
                try {
                    RouteUtils.checkRoutesFiles();
                } catch (Exception e1) {
                    LogUtils.error(e1);
                }
            }).start();
        }*/
    }
}