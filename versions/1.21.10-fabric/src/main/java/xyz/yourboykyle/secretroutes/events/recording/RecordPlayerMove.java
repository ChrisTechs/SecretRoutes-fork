package xyz.yourboykyle.secretroutes.events.recording;

import net.minecraft.util.math.BlockPos;
import xyz.yourboykyle.secretroutes.routes.data.WaypointType;
import xyz.yourboykyle.secretroutes.routes.recording.RouteRecorder;

public class RecordPlayerMove {
    public static void onPlayerMove(BlockPos playerBlockPos) {
        RouteRecorder recorder = RouteRecorder.get();
        if (!recorder.recording) return;

        if (recorder.previousLocation == null) {
            recorder.addWaypoint(WaypointType.LOCATIONS, playerBlockPos);
            recorder.previousLocation = playerBlockPos;
        } else {
            BlockPos prevPos = recorder.previousLocation;
            double distance = Math.sqrt(prevPos.getSquaredDistance(playerBlockPos));

            if (distance >= 2.4) {
                recorder.addWaypoint(WaypointType.LOCATIONS, playerBlockPos);
                recorder.previousLocation = playerBlockPos;
            }
        }
    }
}