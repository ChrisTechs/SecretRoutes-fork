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
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import xyz.yourboykyle.secretroutes.config.SRMConfig;
import xyz.yourboykyle.secretroutes.events.recording.RecordItemPickup;
import xyz.yourboykyle.secretroutes.routes.utils.RoomDirectionUtils;
import xyz.yourboykyle.secretroutes.routes.utils.RoomRotationUtils;
import xyz.yourboykyle.secretroutes.routes.SecretRoutesManager;
import xyz.yourboykyle.secretroutes.routes.data.RouteStep;
import xyz.yourboykyle.secretroutes.routes.data.Secret;
import xyz.yourboykyle.secretroutes.routes.data.SecretCategory;
import xyz.yourboykyle.secretroutes.routes.recording.RouteRecorder;
import xyz.yourboykyle.secretroutes.utils.*;

import java.util.*;

public class OnItemPickedUp {

    public static final Set<String> VALID_ITEMS = Set.of(
            "Decoy", "Defuse Kit", "Dungeon Chest Key", "Healing VIII",
            "Inflatable Jerry", "Spirit Leap", "Training Weights",
            "Trap", "Treasure Talisman"
    );

    private static final Map<String, Integer> previousInventory = new HashMap<>();
    private static final Map<String, Integer> currentInventory = new HashMap<>();
    private static int tickCounter = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(OnItemPickedUp::onClientTick);
    }

    private static void onClientTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        if (++tickCounter % 5 != 0) return;
        if (!LocationUtils.isInDungeons()) return;

        scanInventory(player);
    }

    private static void scanInventory(ClientPlayerEntity player) {
        currentInventory.clear();
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack != null && !stack.isEmpty()) {
                String itemName = stack.getName().getString();
                if (!VALID_ITEMS.contains(itemName)) continue;
                currentInventory.put(itemName, currentInventory.getOrDefault(itemName, 0) + stack.getCount());
            }
        }

        for (Map.Entry<String, Integer> entry : currentInventory.entrySet()) {
            if (entry.getValue() > previousInventory.getOrDefault(entry.getKey(), 0) && isSecretItem(entry.getKey())) {
                handleItemPickup(player, entry.getKey());
            }
        }
        previousInventory.clear();
        previousInventory.putAll(currentInventory);
    }

    public static void handleItemPickup(ClientPlayerEntity player, ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getStack();
        if (stack == null || stack.isEmpty()) return;
        handleItemPickup(player, stack.getName().getString());
    }

    private static void handleItemPickup(ClientPlayerEntity player, String itemName) {
        SecretRoutesManager manager = SecretRoutesManager.get();
        if (manager.getRoomName() == null) return;

        BlockPos playerPos = player.getBlockPos();
        BlockPos playerRelPos = RoomRotationUtils.actualToRelative(playerPos, RoomDirectionUtils.roomDirection(), RoomDirectionUtils.roomCorner());

        // Mark secrets as found based on proximity
        if (SRMConfig.get().allSecrets) {
            for (Secret secret : manager.getAllSecrets()) {
                if (BlockUtils.isWithinRange(playerRelPos, secret.pos(), 5)) {
                    manager.markSecretFound(secret.pos());
                }
            }
        }

        // Auto next
        RouteStep step = manager.getCurrentStep();
        if (step != null && step.targetSecret() != null && step.targetSecret().is(SecretCategory.ITEM)) {
            if (BlockUtils.isWithinRange(playerRelPos, step.targetSecret().pos(), 10)) {
                manager.nextSecret();
                SecretSounds.secretChime();
                LogUtils.info("Picked up item at " + step.targetSecret().pos());
            }
        }

        if (RouteRecorder.get().recording && isSecretItem(itemName))
            RecordItemPickup.onItemPickup(playerPos);
    }

    private static boolean isSecretItem(String itemName) {
        return VALID_ITEMS.contains(Formatting.strip(itemName).trim());
    }
}