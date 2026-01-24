package xyz.yourboykyle.secretroutes.routes.data;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d; // Add this import

import java.util.Collections;
import java.util.List;

public record RouteStep(
        List<BlockPos> etherwarps,
        List<BlockPos> mines,
        List<BlockPos> interacts,
        List<BlockPos> superbooms,
        List<Vec3d> enderpearls,
        List<List<Float>> enderpearlangles,
        List<BlockPos> lines,
        Secret targetSecret,
        String secretType
) {
    public static RouteStep empty() {
        return new RouteStep(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                "none"
        );
    }
}