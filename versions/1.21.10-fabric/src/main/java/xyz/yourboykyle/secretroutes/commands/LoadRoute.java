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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import xyz.yourboykyle.secretroutes.routes.utils.RoomDirectionUtils;
import xyz.yourboykyle.secretroutes.routes.SecretRoutesManager;
import xyz.yourboykyle.secretroutes.routes.data.RouteStep;
import xyz.yourboykyle.secretroutes.routes.data.Secret;
import xyz.yourboykyle.secretroutes.routes.data.SecretCategory;
import xyz.yourboykyle.secretroutes.utils.LogUtils;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static xyz.yourboykyle.secretroutes.utils.JsonUtils.*;

public class LoadRoute {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(LoadRoute::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(literal("loadroute").executes(LoadRoute::executeCommand));
    }

    private static int executeCommand(CommandContext<FabricClientCommandSource> context) {
        String filePath = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + "routes.json";
        File file = new File(filePath);

        if (!file.exists()) {
            context.getSource().sendError(Text.literal("File not found in Downloads: routes.json"));
            return 0;
        }

        try (FileReader reader = new FileReader(file)) {
            JsonObject root = new Gson().fromJson(reader, JsonObject.class);
            String currentRoom = RoomDirectionUtils.roomName();

            if (root.has(currentRoom)) {
                List<RouteStep> steps = parseRouteSteps(root.getAsJsonArray(currentRoom));
                SecretRoutesManager.get().overrideRoute(currentRoom, steps);
                context.getSource().sendFeedback(Text.literal("Loaded local route for: " + currentRoom).formatted(Formatting.GREEN));
            } else {
                context.getSource().sendError(Text.literal("Room not found in local file: " + currentRoom));
            }
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Failed to load route: " + e.getMessage()));
            LogUtils.error(e);
        }

        return 1;
    }

    private static List<RouteStep> parseRouteSteps(JsonArray array) {
        List<RouteStep> steps = new ArrayList<>();
        for (JsonElement el : array) {
            JsonObject obj = el.getAsJsonObject();
            Secret targetSecret = null;
            String secretType = "none";

            if (obj.has("secret")) {
                JsonObject sObj = obj.getAsJsonObject("secret");
                if (sObj.has("type")) secretType = sObj.get("type").getAsString();

                if (sObj.has("location")) {
                    JsonArray loc = sObj.getAsJsonArray("location");
                    targetSecret = new Secret("Target", SecretCategory.UNKNOWN,
                            new BlockPos(loc.get(0).getAsInt(), loc.get(1).getAsInt(), loc.get(2).getAsInt()));
                }
            }

            steps.add(new RouteStep(
                    toPosList(obj.getAsJsonArray("etherwarps")),
                    toPosList(obj.getAsJsonArray("mines")),
                    toPosList(obj.getAsJsonArray("interacts")),
                    toPosList(obj.getAsJsonArray("tnts")),
                    toVec3dList(obj.getAsJsonArray("enderpearls")),
                    toFloatListList(obj.getAsJsonArray("enderpearlangles")),
                    toPosList(obj.getAsJsonArray("locations")),
                    targetSecret,
                    secretType
            ));
        }
        return steps;
    }
}