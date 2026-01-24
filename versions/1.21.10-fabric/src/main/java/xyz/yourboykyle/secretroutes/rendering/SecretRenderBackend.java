package xyz.yourboykyle.secretroutes.rendering;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.yourboykyle.secretroutes.Main;
import xyz.yourboykyle.secretroutes.config.SRMConfig;
import xyz.yourboykyle.secretroutes.events.OnWorldRender;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class SecretRenderBackend {
    private static final List<WorldText> textQueue = new ArrayList<>();
    private static final List<OutlinedBox> outlinedQueue = new ArrayList<>();
    private static final List<FilledBox> filledQueue = new ArrayList<>();
    private static final List<Line> lineQueue = new ArrayList<>();

    private static final RenderPipeline SEE_THROUGH_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(Identifier.of(Main.MODID, "see_through_overlay"))
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withCull(false)
                    .build()
    );
    private static final RenderLayer SEE_THROUGH_LAYER = RenderLayer.of("secretroutes_see_through", 1536, SEE_THROUGH_PIPELINE,
            RenderLayer.MultiPhaseParameters.builder().target(RenderLayer.MAIN_TARGET).build(false));

    private static final RenderPipeline NORMAL_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(Identifier.of(Main.MODID, "normal_overlay"))
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withCull(false)
                    .build()
    );
    private static final RenderLayer NORMAL_LAYER = RenderLayer.of("secretroutes_normal", 1536, NORMAL_PIPELINE,
            RenderLayer.MultiPhaseParameters.builder().target(RenderLayer.MAIN_TARGET).build(false));


    public static void register() {
        WorldRenderEvents.END_MAIN.register(SecretRenderBackend::render);
    }

    public static void addBox(Vec3d pos, Color color, boolean filled) {
        if (filled) filledQueue.add(new FilledBox(pos, color, true));
        else outlinedQueue.add(new OutlinedBox(pos, color, true));
    }

    public static void addLine(Vec3d start, Vec3d end, Color color, float width) {
        boolean throughWalls = SRMConfig.get().renderLinesThroughWalls;
        lineQueue.add(new Line(start, end, color, width, throughWalls));
    }

    public static void addText(String text, Vec3d pos, int color, float scale) {
        textQueue.add(new WorldText(text, pos, true, scale, color));
    }

    public static void spawnParticle(BlockPos pos, ParticleEffect particle) {
        spawnParticle(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), particle);
    }

    public static void spawnParticle(Vec3d pos, ParticleEffect particle) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world != null) {
            mc.world.addParticleClient(particle, true, true, pos.getX(), pos.getY(), pos.getZ(), 0, 0, 0);
        }
    }

    private static void render(WorldRenderContext context) {
        OnWorldRender.onRenderWorld();

        if (textQueue.isEmpty() && outlinedQueue.isEmpty() && filledQueue.isEmpty() && lineQueue.isEmpty()) return;

        try {
            MatrixStack matrices = context.matrices();
            Vec3d camPos = context.worldState().cameraRenderState.pos;

            try (BufferAllocator allocator = new BufferAllocator(196608)) {
                VertexConsumerProvider.Immediate consumers = VertexConsumerProvider.immediate(allocator);

                renderGeometry(consumers, matrices, camPos, true, SEE_THROUGH_LAYER);

                renderGeometry(consumers, matrices, camPos, false, NORMAL_LAYER);

                renderTextBatch(consumers, matrices, camPos);

                consumers.draw();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            textQueue.clear();
            outlinedQueue.clear();
            filledQueue.clear();
            lineQueue.clear();
        }
    }

    private static void renderGeometry(VertexConsumerProvider consumers, MatrixStack matrices, Vec3d camPos, boolean throughWalls, RenderLayer layer) {
        VertexConsumer buffer = consumers.getBuffer(layer);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Filled Boxes
        float alpha = SRMConfig.get().filledBoxAlpha;
        for (FilledBox fb : filledQueue) {
            if (fb.throughWalls() != throughWalls) continue;
            drawBoxFaces(buffer, matrix, getBox(fb.pos(), camPos), fb.color(), alpha);
        }

        // Outlined Boxes
        float t = Math.max(0.002f, SRMConfig.get().boxLineWidth * 0.01f);
        for (OutlinedBox ob : outlinedQueue) {
            if (ob.throughWalls() != throughWalls) continue;
            drawBoxEdges(buffer, matrix, getBox(ob.pos(), camPos), ob.color(), 1.0f, t);
        }

        // Lines
        for (Line l : lineQueue) {
            if (l.throughWalls() != throughWalls) continue;
            float thickness = Math.max(0.002f, l.width() * 0.01f);
            drawBillboardLine(buffer, matrix, l.start().subtract(camPos), l.end().subtract(camPos), l.color(), 1.0f, thickness);
        }
    }

    private static void renderTextBatch(VertexConsumerProvider.Immediate consumers, MatrixStack matrices, Vec3d camPos) {
        if (textQueue.isEmpty()) return;

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        Quaternionf cameraRotation = MinecraftClient.getInstance().gameRenderer.getCamera().getRotation();

        for (WorldText wt : textQueue) {
            matrices.push();
            matrices.translate(wt.pos().x, wt.pos().y, wt.pos().z);
            matrices.translate(-camPos.x, -camPos.y, -camPos.z);
            matrices.multiply(cameraRotation);

            float s = wt.scale() * 0.025f;
            matrices.scale(s, -s, s);

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float xOffset = -textRenderer.getWidth(wt.text()) / 2f;

            int color = wt.color();
            if ((color & 0xFF000000) == 0) color |= 0xFF000000;

            TextRenderer.TextLayerType layerType = wt.throughWalls()
                    ? TextRenderer.TextLayerType.SEE_THROUGH
                    : TextRenderer.TextLayerType.NORMAL;

            textRenderer.draw(wt.text(), xOffset, 0, color, true, matrix, consumers, layerType, 0, 0xF000F0);
            matrices.pop();
        }
    }

    private static Box getBox(Vec3d pos, Vec3d camPos) {
        return new Box(pos.x, pos.y, pos.z, pos.x + 1, pos.y + 1, pos.z + 1).offset(camPos.negate());
    }

    private static void drawBillboardLine(VertexConsumer buffer, Matrix4f mat, Vec3d start, Vec3d end, Color c, float a, float t) {
        float r = c.getRed()/255f; float g = c.getGreen()/255f; float b = c.getBlue()/255f;
        Vector3f lineDir = new Vector3f((float)(end.x-start.x), (float)(end.y-start.y), (float)(end.z-start.z)).normalize();
        Vector3f camDir = new Vector3f((float)start.x, (float)start.y, (float)start.z);
        if(camDir.lengthSquared() < 0.0001f) camDir.set(0,1,0);
        camDir.normalize();
        Vector3f widthDir = lineDir.cross(camDir, new Vector3f());
        if(widthDir.lengthSquared() < 0.0001f) widthDir.set(1,0,0);
        widthDir.normalize().mul(t/2.0f);

        buffer.vertex(mat, (float)start.x-widthDir.x, (float)start.y-widthDir.y, (float)start.z-widthDir.z).color(r,g,b,a).light(15728880);
        buffer.vertex(mat, (float)start.x+widthDir.x, (float)start.y+widthDir.y, (float)start.z+widthDir.z).color(r,g,b,a).light(15728880);
        buffer.vertex(mat, (float)end.x+widthDir.x, (float)end.y+widthDir.y, (float)end.z+widthDir.z).color(r,g,b,a).light(15728880);
        buffer.vertex(mat, (float)end.x-widthDir.x, (float)end.y-widthDir.y, (float)end.z-widthDir.z).color(r,g,b,a).light(15728880);
    }
    private static void drawBoxFaces(VertexConsumer buffer, Matrix4f mat, Box box, Color c, float a) {
        float r = c.getRed()/255f; float g = c.getGreen()/255f; float b = c.getBlue()/255f;
        float x1 = (float)box.minX; float x2 = (float)box.maxX; float y1 = (float)box.minY; float y2 = (float)box.maxY; float z1 = (float)box.minZ; float z2 = (float)box.maxZ; int l = 15728880;
        buffer.vertex(mat, x1, y1, z2).color(r, g, b, a).light(l); buffer.vertex(mat, x1, y1, z1).color(r, g, b, a).light(l);
        buffer.vertex(mat, x2, y1, z1).color(r, g, b, a).light(l); buffer.vertex(mat, x2, y1, z2).color(r, g, b, a).light(l);
        buffer.vertex(mat, x1, y2, z1).color(r, g, b, a).light(l); buffer.vertex(mat, x1, y2, z2).color(r, g, b, a).light(l);
        buffer.vertex(mat, x2, y2, z2).color(r, g, b, a).light(l); buffer.vertex(mat, x2, y2, z1).color(r, g, b, a).light(l);
        buffer.vertex(mat, x1, y1, z1).color(r, g, b, a).light(l); buffer.vertex(mat, x1, y2, z1).color(r, g, b, a).light(l);
        buffer.vertex(mat, x2, y2, z1).color(r, g, b, a).light(l); buffer.vertex(mat, x2, y1, z1).color(r, g, b, a).light(l);
        buffer.vertex(mat, x2, y1, z2).color(r, g, b, a).light(l); buffer.vertex(mat, x2, y2, z2).color(r, g, b, a).light(l);
        buffer.vertex(mat, x1, y2, z2).color(r, g, b, a).light(l); buffer.vertex(mat, x1, y1, z2).color(r, g, b, a).light(l);
        buffer.vertex(mat, x1, y1, z2).color(r, g, b, a).light(l); buffer.vertex(mat, x1, y2, z2).color(r, g, b, a).light(l);
        buffer.vertex(mat, x1, y2, z1).color(r, g, b, a).light(l); buffer.vertex(mat, x1, y1, z1).color(r, g, b, a).light(l);
        buffer.vertex(mat, x2, y1, z1).color(r, g, b, a).light(l); buffer.vertex(mat, x2, y2, z1).color(r, g, b, a).light(l);
        buffer.vertex(mat, x2, y2, z2).color(r, g, b, a).light(l); buffer.vertex(mat, x2, y1, z2).color(r, g, b, a).light(l);
    }
    private static void drawBoxEdges(VertexConsumer buffer, Matrix4f mat, Box box, Color c, float a, float t) {
        Vec3d[] v = {
                new Vec3d(box.minX, box.minY, box.minZ), new Vec3d(box.maxX, box.minY, box.minZ), new Vec3d(box.minX, box.maxY, box.minZ), new Vec3d(box.maxX, box.maxY, box.minZ),
                new Vec3d(box.minX, box.minY, box.maxZ), new Vec3d(box.maxX, box.minY, box.maxZ), new Vec3d(box.minX, box.maxY, box.maxZ), new Vec3d(box.maxX, box.maxY, box.maxZ)
        };
        drawBillboardLine(buffer, mat, v[0], v[1], c, a, t); drawBillboardLine(buffer, mat, v[2], v[3], c, a, t); drawBillboardLine(buffer, mat, v[4], v[5], c, a, t); drawBillboardLine(buffer, mat, v[6], v[7], c, a, t);
        drawBillboardLine(buffer, mat, v[0], v[2], c, a, t); drawBillboardLine(buffer, mat, v[1], v[3], c, a, t); drawBillboardLine(buffer, mat, v[4], v[6], c, a, t); drawBillboardLine(buffer, mat, v[5], v[7], c, a, t);
        drawBillboardLine(buffer, mat, v[0], v[4], c, a, t); drawBillboardLine(buffer, mat, v[1], v[5], c, a, t); drawBillboardLine(buffer, mat, v[2], v[6], c, a, t); drawBillboardLine(buffer, mat, v[3], v[7], c, a, t);
    }
}