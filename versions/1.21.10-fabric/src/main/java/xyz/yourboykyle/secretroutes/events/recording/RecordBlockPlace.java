package xyz.yourboykyle.secretroutes.events.recording;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import xyz.yourboykyle.secretroutes.routes.data.WaypointType;
import xyz.yourboykyle.secretroutes.routes.recording.RouteRecorder;

public class RecordBlockPlace {
    public static void onBlockPlace(BlockPos pos, BlockState blockState) {
        RouteRecorder recorder = RouteRecorder.get();
        if (blockState.getBlock() == Blocks.TNT && recorder.recording) {
            if (recorder.addWaypoint(WaypointType.TNTS, pos)) {
                recorder.setRecordingMessage("Added TNT waypoint.");
            }
        }
    }
}