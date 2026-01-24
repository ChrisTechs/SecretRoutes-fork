package xyz.yourboykyle.secretroutes.events;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import xyz.yourboykyle.secretroutes.config.SRMConfig;
import xyz.yourboykyle.secretroutes.routes.SecretRoutesManager;
import xyz.yourboykyle.secretroutes.utils.GuiUtils;

public class OnGuiRender {
    public static Long spawnNotifTime = null;

    public static void register() {
        HudRenderCallback.EVENT.register(OnGuiRender::onHudRender);
    }

    private static void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        if (SecretRoutesManager.get().shouldShowLeverBanner()) {
            GuiUtils.displayText(drawContext, "§bSet waypoint at lever", 0, -100, 2);
        }

        if (spawnNotifTime != null || SRMConfig.get().renderBlood) {
            if (SRMConfig.get().renderBlood || System.currentTimeMillis() < spawnNotifTime) {
                String text = SRMConfig.get().bloodReadyColor.formatting + SRMConfig.get().bloodReadyText;

                GuiUtils.displayText(
                        drawContext,
                        text,
                        SRMConfig.get().bloodX,
                        SRMConfig.get().bloodY,
                        SRMConfig.get().bloodScale
                );
            } else {
                spawnNotifTime = null;
            }
        }
    }
}