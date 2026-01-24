package xyz.yourboykyle.secretroutes.rendering;

import net.minecraft.util.math.Vec3d;

public record WorldText(String text, Vec3d pos, boolean throughWalls, float scale, int color) {}

