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

package xyz.yourboykyle.secretroutes.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JsonUtils {
    public static JsonObject toJsonObject(String string) {
        JsonParser parser = new JsonParser();
        return parser.parse(string).getAsJsonObject();
    }

    public static List<List<Float>> toFloatListList(JsonArray arr) {
        if (arr == null) return Collections.emptyList();
        List<List<Float>> list = new ArrayList<>();
        for (JsonElement el : arr) {
            JsonArray p = el.getAsJsonArray();
            List<Float> angles = new ArrayList<>();
            angles.add(p.get(0).getAsFloat());
            angles.add(p.get(1).getAsFloat());
            list.add(angles);
        }
        return list;
    }

    public static List<Vec3d> toVec3dList(JsonArray arr) {
        if (arr == null) return Collections.emptyList();
        List<Vec3d> list = new ArrayList<>();
        for (JsonElement el : arr) {
            JsonArray p = el.getAsJsonArray();
            list.add(new Vec3d(p.get(0).getAsDouble(), p.get(1).getAsDouble(), p.get(2).getAsDouble()));
        }
        return list;
    }

    public static List<BlockPos> toPosList(JsonArray arr) {
        if (arr == null) return Collections.emptyList();
        List<BlockPos> list = new ArrayList<>();
        for (JsonElement el : arr) {
            JsonArray p = el.getAsJsonArray();
            list.add(new BlockPos(p.get(0).getAsInt(), p.get(1).getAsInt(), p.get(2).getAsInt()));
        }
        return list;
    }
}
