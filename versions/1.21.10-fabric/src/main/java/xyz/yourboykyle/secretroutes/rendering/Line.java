package xyz.yourboykyle.secretroutes.rendering;

import net.minecraft.util.math.Vec3d;

import java.awt.*;

public record Line(Vec3d start, Vec3d end, Color color, float width, boolean throughWalls) {}
