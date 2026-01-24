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

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xyz.yourboykyle.secretroutes.config.SRMConfig;
import xyz.yourboykyle.secretroutes.routes.utils.RoomDirectionUtils;
import xyz.yourboykyle.secretroutes.routes.utils.RoomRotationUtils;
import xyz.yourboykyle.secretroutes.routes.SecretRoutesManager;
import xyz.yourboykyle.secretroutes.routes.data.RouteStep;
import xyz.yourboykyle.secretroutes.routes.data.Secret;
import xyz.yourboykyle.secretroutes.routes.data.SecretCategory;
import xyz.yourboykyle.secretroutes.events.recording.RecordPlayerInteract;
import xyz.yourboykyle.secretroutes.routes.recording.RouteRecorder;
import xyz.yourboykyle.secretroutes.utils.*;

public class OnPlayerInteract {

    private static final long INTERACT_COOLDOWN = 110;
    private static long lastInteractTime = 0;
    private static BlockPos lastInteractPos = null;

    public static void register() {
        UseBlockCallback.EVENT.register(OnPlayerInteract::onInteract);
    }

    public static BlockPos getLastInteractPos() {
        return lastInteractPos;
    }

    public static ActionResult onInteract(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        try {
            if (hand != Hand.MAIN_HAND || !LocationUtils.isInDungeons()) return ActionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            long currentTime = System.currentTimeMillis();
            if (pos.equals(lastInteractPos) && (currentTime - lastInteractTime) < INTERACT_COOLDOWN) return ActionResult.PASS;

            lastInteractTime = currentTime;
            lastInteractPos = pos;

            Block block = world.getBlockState(pos).getBlock();
            if (block != Blocks.CHEST && block != Blocks.TRAPPED_CHEST && block != Blocks.LEVER && block != Blocks.PLAYER_HEAD && block != Blocks.SKELETON_SKULL) {
                return ActionResult.PASS;
            }

            SecretRoutesManager manager = SecretRoutesManager.get();
            BlockPos relPos = RoomRotationUtils.actualToRelative(pos, RoomDirectionUtils.roomDirection(), RoomDirectionUtils.roomCorner());

            if (manager.getCurrentLeverPos() != null && relPos.equals(manager.getCurrentLeverPos())) {
                manager.resetLeverState();
            }

            if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) {
                manager.handleLockedChestInteract(relPos);
            }

            if (SRMConfig.get().allSecrets) {
                for (Secret secret : manager.getAllSecrets()) {
                    if (secret.pos().equals(relPos)) {
                        manager.markSecretFound(relPos);
                        break;
                    }
                }
            }

            // Auto Next Step
            RouteStep step = manager.getCurrentStep();
            if (step != null && step.targetSecret() != null) {
                SecretCategory cat = step.targetSecret().category();
                if ((cat == SecretCategory.CHEST || cat == SecretCategory.WITHER) && step.targetSecret().pos().equals(relPos)) {
                    SecretSounds.secretChime();
                    manager.nextSecret();
                    LogUtils.info("Interacted with block at " + relPos);
                }
            }

            if (RouteRecorder.get().recording) {
                RecordPlayerInteract.onPlayerInteract(block, pos);
            }
        } catch (Exception ex) {
            LogUtils.error(ex);
        }

        return ActionResult.PASS;
    }
}