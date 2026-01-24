package xyz.yourboykyle.secretroutes.events.recording;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xyz.yourboykyle.secretroutes.routes.SecretRoutesManager;
import xyz.yourboykyle.secretroutes.routes.data.WaypointType;
import xyz.yourboykyle.secretroutes.routes.recording.RouteRecorder;
import xyz.yourboykyle.secretroutes.utils.LogUtils;

public class RecordBlockBreak {
    public static void register() {
        PlayerBlockBreakEvents.AFTER.register(RecordBlockBreak::onBlockBreak);
    }

    public static void handleBlockBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        onBlockBreak(world, player, pos, state, null);
    }

    private static void onBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;

            if (SecretRoutesManager.get().getRoomName() == null) return;

            RouteRecorder recorder = RouteRecorder.get();
            if (player.getUuid().equals(mc.player.getUuid()) && recorder.recording) {

                ItemStack heldItem = player.getMainHandStack();
                boolean isPickaxe = heldItem != null && !heldItem.isEmpty() &&
                        Registries.ITEM.getId(heldItem.getItem()).toString().contains("pickaxe");

                if (isPickaxe) {
                    if (recorder.addWaypoint(WaypointType.MINES, pos)) {
                        recorder.setRecordingMessage("Added mine waypoint.");
                    }
                }
            }
        } catch (Exception ex) {
            LogUtils.error(ex);
        }
    }
}