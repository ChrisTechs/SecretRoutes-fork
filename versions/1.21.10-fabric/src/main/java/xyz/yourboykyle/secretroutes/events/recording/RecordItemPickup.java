package xyz.yourboykyle.secretroutes.events.recording;

import net.minecraft.util.math.BlockPos;
import xyz.yourboykyle.secretroutes.routes.data.SecretCategory;
import xyz.yourboykyle.secretroutes.routes.recording.RouteRecorder;

public class RecordItemPickup {
    public static boolean itemSecretOnCooldown = false;

    public static void onItemPickup(BlockPos playerPos) {
        RouteRecorder recorder = RouteRecorder.get();
        if (!itemSecretOnCooldown && recorder.recording) {
            if (recorder.addSecret(SecretCategory.ITEM, playerPos)) {
                recorder.newSecret();
                recorder.setRecordingMessage("Added item secret waypoint.");
            }
        }
    }
}