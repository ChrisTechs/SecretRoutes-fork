package xyz.yourboykyle.secretroutes.config.huds;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import xyz.yourboykyle.secretroutes.Main;
import xyz.yourboykyle.secretroutes.config.SRMConfig;
import xyz.yourboykyle.secretroutes.routes.recording.RouteRecorder;

public class RecordingHUD {

    public void render(DrawContext context) {
        if (!RouteRecorder.get().recording) {
            return;
        }

        String prefix = "Recording status: ";
        String message = RouteRecorder.get().recordingMessage;
        if (message == null) message = "";
        String fullText = prefix + message;

        int x = SRMConfig.get().recordingHudX;
        int y = SRMConfig.get().recordingHudY;

        int color = SRMConfig.get().recordingHudColor.getRGB();

        context.drawTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                Text.literal(fullText),
                x,
                y,
                color
        );
    }
}