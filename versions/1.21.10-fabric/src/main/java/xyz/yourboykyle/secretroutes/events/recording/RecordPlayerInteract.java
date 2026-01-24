package xyz.yourboykyle.secretroutes.events.recording;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import xyz.yourboykyle.secretroutes.routes.data.SecretCategory;
import xyz.yourboykyle.secretroutes.routes.data.WaypointType;
import xyz.yourboykyle.secretroutes.routes.recording.RouteRecorder;
import xyz.yourboykyle.secretroutes.utils.LogUtils;

public class RecordPlayerInteract {
    public static void onPlayerInteract(Block block, BlockPos pos) {
        RouteRecorder recorder = RouteRecorder.get();
        if (!recorder.recording) return;

        if (block == Blocks.LEVER) {
            if (recorder.addWaypoint(WaypointType.INTERACTS, pos)) {
                recorder.setRecordingMessage("Added interact waypoint.");
            }
        } else {
            if (recorder.addSecret(SecretCategory.CHEST, pos)) {
                recorder.newSecret();
                recorder.setRecordingMessage("Added interact secret waypoint.");

                RecordItemPickup.itemSecretOnCooldown = true;
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        RecordItemPickup.itemSecretOnCooldown = false;
                    } catch (InterruptedException ignored) {}
                }).start();
            }
        }
    }
}