/*
 * Secret Routes Mod - Secret Route Waypoints for Hypixel Skyblock Dungeons
 * Copyright 2024 yourboykyle & R-aMcC
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

package xyz.yourboykyle.secretroutes.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import xyz.yourboykyle.secretroutes.config.SRMConfig;
import xyz.yourboykyle.secretroutes.routes.utils.RoomDirectionUtils;
import xyz.yourboykyle.secretroutes.routes.data.SecretCategory;
import xyz.yourboykyle.secretroutes.routes.recording.RouteRecorder;
import xyz.yourboykyle.secretroutes.utils.LogUtils;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class Recording {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(Recording::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(literal("recording")
                .executes(Recording::openGui)
                .then(literal("start").executes(Recording::executeStart))
                .then(literal("stop").executes(Recording::executeStop))
                .then(literal("export").executes(Recording::executeExport))
                .then(literal("getroom").executes(Recording::executeGetRoom))
                .then(literal("setbat").executes(Recording::executeSetBat))
                .then(literal("setitem").executes(Recording::executeSetItem))
                .then(literal("setexit").executes(Recording::executeSetExit))
                .then(literal("import")
                        .then(argument("filename", StringArgumentType.greedyString())
                                .executes(Recording::executeImport))));
    }

    private static int openGui(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.send(() -> client.setScreen(SRMConfig.getScreen(client.currentScreen)));
        return 1;
    }

    private static int executeStart(CommandContext<FabricClientCommandSource> context) {
        RouteRecorder.get().startRecording();
        return 1;
    }

    private static int executeStop(CommandContext<FabricClientCommandSource> context) {
        RouteRecorder.get().stopRecording();
        return 1;
    }

    private static int executeExport(CommandContext<FabricClientCommandSource> context) {
        RouteRecorder.get().exportAllRoutes();
        return 1;
    }

    private static int executeGetRoom(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(
                Text.literal("Room Name: " + RoomDirectionUtils.roomName() +
                                ", Room Corner: " + RoomDirectionUtils.roomCorner() +
                                ", Room Direction: " + RoomDirectionUtils.roomDirection())
                        .formatted(Formatting.BLUE)
        );
        return 1;
    }

    private static int executeSetBat(CommandContext<FabricClientCommandSource> context) {
        if (RouteRecorder.get().recording) {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) return 0;

            BlockPos targetPos = player.getBlockPos().add(-1, 2, -1);

            RouteRecorder.get().addSecret(SecretCategory.BAT, targetPos);
            RouteRecorder.get().newSecret();
            RouteRecorder.get().setRecordingMessage("Added bat secret waypoint.");
        } else {
            context.getSource().sendError(Text.literal("Route recording is not enabled. Run /recording start"));
        }
        return 1;
    }

    private static int executeSetItem(CommandContext<FabricClientCommandSource> context) {
        if (RouteRecorder.get().recording) {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) return 0;

            BlockPos targetPos = player.getBlockPos();

            RouteRecorder.get().addSecret(SecretCategory.ITEM, targetPos);
            RouteRecorder.get().newSecret();
            RouteRecorder.get().setRecordingMessage("Added item secret waypoint.");
        } else {
            context.getSource().sendError(Text.literal("Route recording is not enabled. Run /recording start"));
        }
        return 1;
    }

    private static int executeSetExit(CommandContext<FabricClientCommandSource> context) {
        if (RouteRecorder.get().recording) {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) return 0;

            BlockPos targetPos = player.getBlockPos().add(-1, 0, -1);

            // TODO : Exit route type add

            RouteRecorder.get().stopRecording();
            RouteRecorder.get().setRecordingMessage("Added route exit waypoint & stopped recording.");
            LogUtils.info("Added route exit waypoint & stopped recording.");
        } else {
            context.getSource().sendError(Text.literal("Route recording is not enabled. Run /recording start"));
        }
        return 1;
    }

    private static int executeImport(CommandContext<FabricClientCommandSource> context) {
        String filename = StringArgumentType.getString(context, "filename");
        RouteRecorder.get().importRoutes(filename);
        context.getSource().sendFeedback(
                Text.literal("Imported routes from ").formatted(Formatting.DARK_GREEN)
                        .append(Text.literal(filename).formatted(Formatting.GREEN))
        );
        return 1;
    }
}