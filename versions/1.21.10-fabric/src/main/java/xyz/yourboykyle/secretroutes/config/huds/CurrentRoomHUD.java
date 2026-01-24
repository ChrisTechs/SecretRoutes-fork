package xyz.yourboykyle.secretroutes.config.huds;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import xyz.yourboykyle.secretroutes.routes.utils.RoomDirectionUtils;
import xyz.yourboykyle.secretroutes.routes.recording.RouteRecorder;

public class CurrentRoomHUD {

    public void render(DrawContext context) {
        if (!RouteRecorder.get().recording) return;

        String text = "Room: " + RoomDirectionUtils.roomName();
        int x = 10;
        int y = 10;

        context.drawText(MinecraftClient.getInstance().textRenderer, text, x, y, 0xFFFFFF, true);
    }
}