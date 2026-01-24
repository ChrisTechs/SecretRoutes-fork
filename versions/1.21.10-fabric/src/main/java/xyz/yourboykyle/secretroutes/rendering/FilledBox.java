package xyz.yourboykyle.secretroutes.rendering;

import net.minecraft.util.math.Vec3d;

import java.awt.*;

public record FilledBox(Vec3d pos, Color color, boolean throughWalls) {}
