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

package xyz.yourboykyle.secretroutes.events;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import xyz.yourboykyle.secretroutes.events.recording.RecordPlayerMove;
import xyz.yourboykyle.secretroutes.routes.utils.RoomDirectionUtils;
import xyz.yourboykyle.secretroutes.routes.utils.RoomRotationUtils;
import xyz.yourboykyle.secretroutes.routes.SecretRoutesManager;
import xyz.yourboykyle.secretroutes.routes.data.RouteStep;
import xyz.yourboykyle.secretroutes.routes.data.SecretCategory;
import xyz.yourboykyle.secretroutes.routes.recording.RouteRecorder;
import xyz.yourboykyle.secretroutes.utils.BlockUtils;
import xyz.yourboykyle.secretroutes.utils.LogUtils;

public class OnPlayerTick {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(OnPlayerTick::onPlayerTick);
    }

    private static long itemHoverStart = 0;
    private static int lastSecretIdx = -1;

    private static void onPlayerTick(MinecraftClient client) {
        try {
            ClientPlayerEntity player = client.player;
            if (player == null) return;

            SecretRoutesManager manager = SecretRoutesManager.get();
            if (manager.getRoomName() == null) return;

            RouteStep step = manager.getCurrentStep();
            if (step != null && step.targetSecret() != null) {
                BlockPos secretRelPos = step.targetSecret().pos();
                BlockPos playerRel = RoomRotationUtils.actualToRelative(player.getBlockPos(), RoomDirectionUtils.roomDirection(), RoomDirectionUtils.roomCorner());

                int currentIndex = manager.getCurrentStepIndex();
                if (currentIndex != lastSecretIdx) itemHoverStart = 0;

                SecretCategory type = step.targetSecret().category();

                if (type == SecretCategory.BAT) {
                    if (BlockUtils.isWithinRange(playerRel, secretRelPos, 3)) {
                        manager.nextSecret();
                        LogUtils.info("Went by bat at " + secretRelPos);
                        itemHoverStart = 0;
                    }
                } else if (type == SecretCategory.ITEM) {
                    if (BlockUtils.isWithinRange(playerRel, secretRelPos, 2)) {
                        if (itemHoverStart == 0) {
                            itemHoverStart = System.currentTimeMillis() + 1500;
                        } else if (System.currentTimeMillis() >= itemHoverStart) {
                            manager.nextSecret();
                            LogUtils.info("Picked up item at " + secretRelPos + " (Auto)");
                            itemHoverStart = 0;
                        }
                    } else {
                        itemHoverStart = 0;
                    }
                } else {
                    itemHoverStart = 0;
                }

                lastSecretIdx = currentIndex;
            }

            if (RouteRecorder.get().recording) {
                RecordPlayerMove.onPlayerMove(player.getBlockPos());
            }
        } catch (Exception e) {
            LogUtils.error(e);
        }
    }
}