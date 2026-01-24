package xyz.yourboykyle.secretroutes.events.recording;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xyz.yourboykyle.secretroutes.routes.data.WaypointType;
import xyz.yourboykyle.secretroutes.routes.recording.RouteRecorder;
import xyz.yourboykyle.secretroutes.utils.LocationUtils;
import xyz.yourboykyle.secretroutes.utils.LogUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RecordPlaySound {
    private static final ConcurrentMap<String, Long> recentSounds = new ConcurrentHashMap<>();
    private static final long SOUND_COOLDOWN = 100;

    // Called from ClientPlayNetworkHandlerMixin
    public static void handleSoundPlayed(PlaySoundS2CPacket packet) {
        if (!LocationUtils.isInDungeons() || !RouteRecorder.get().recording) return;
        try {
            RegistryEntry<SoundEvent> soundEvent = packet.getSound();
            if (soundEvent == null || soundEvent.getIdAsString() == null) return;

            String soundPath = soundEvent.getIdAsString();

            long currentTime = System.currentTimeMillis();
            Long lastProcessed = recentSounds.get(soundPath);
            if (lastProcessed != null && currentTime - lastProcessed < SOUND_COOLDOWN) return;
            recentSounds.put(soundPath, currentTime);

            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player == null) return;

            System.out.println(soundPath);

            // Etherwarp Detection
            if (soundPath.contains("ender_dragon.hurt") || soundPath.contains("enderdragon.hit")) {
                RouteRecorder recorder = RouteRecorder.get();
                if (player.isSneaking() && player.getMainHandStack().getItem() == Items.DIAMOND_SHOVEL) {
                    player.sendMessage(Text.literal("Detected etherwarp! Please wait 0.5 seconds..."), false);
                    recorder.setRecordingMessage("Detected etherwarp! Please wait 0.5 seconds...");
                    new Thread(() -> {
                        try {
                            Thread.sleep(500);
                            BlockPos targetPos = player.getBlockPos().add(-1, -1, -1);
                            recorder.addWaypoint(WaypointType.ETHERWARPS, targetPos);
                            recorder.setRecordingMessage("Etherwarp recorded!");
                        } catch (InterruptedException ex) {
                            LogUtils.error(ex);
                        }
                    }).start();
                }
            }

            // Block Break Detection
            if (soundPath.contains("block") && soundPath.contains("break")) {
                BlockPos pos = new BlockPos((int) packet.getX(), (int) packet.getY(), (int) packet.getZ());
                World world = client.world;
                if (world != null) {
                    BlockState blockState = world.getBlockState(pos);
                    RecordBlockBreak.handleBlockBreak(world, pos, blockState, player);
                }
            }
        } catch (Exception ex) {
            LogUtils.error(ex);
        }
    }
}