package xyz.yourboykyle.secretroutes.routes.data;

import net.minecraft.util.math.BlockPos;

public record Secret(
        String name,
        SecretCategory category,
        BlockPos pos
) {
    public boolean is(SecretCategory c) {
        return this.category == c;
    }
}