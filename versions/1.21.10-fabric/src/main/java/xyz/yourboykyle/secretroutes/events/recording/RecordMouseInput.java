package xyz.yourboykyle.secretroutes.events.recording;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import xyz.yourboykyle.secretroutes.routes.data.WaypointType;
import xyz.yourboykyle.secretroutes.routes.recording.RouteRecorder;
import xyz.yourboykyle.secretroutes.utils.LogUtils;

public class RecordMouseInput {
    private static boolean[] previousMouseState = new boolean[8];

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player == null || mc.mouse == null) return;

                boolean rightClick = mc.mouse.wasRightButtonClicked();
                if (rightClick && !previousMouseState[1]) {
                    onMouseClick(mc.player, 1);
                }
                previousMouseState[1] = rightClick;
            } catch (Exception ex) {
                LogUtils.error(ex);
            }
        });
    }

    private static void onMouseClick(ClientPlayerEntity player, int button) {
        try {
            ItemStack item = player.getMainHandStack();
            if (item.isEmpty()) return;

            String itemName = item.getName().getString().toLowerCase();
            RouteRecorder recorder = RouteRecorder.get();
            if (!recorder.recording) return;

            if (itemName.contains("ender pearl") && button == 1) {
                recorder.addWaypoint(WaypointType.ENDERPEARLS, player);
            } else if (itemName.contains("boom")) {
                recorder.addWaypoint(WaypointType.TNTS, player.getBlockPos());
                recorder.setRecordingMessage("Added TNT (Boom) waypoint.");
            }
        } catch (Exception ex) {
            LogUtils.error(ex);
        }
    }
}