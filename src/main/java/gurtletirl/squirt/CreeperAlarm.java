package gurtletirl.squirt;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class CreeperAlarm implements ClientModInitializer, HudElement {

    private static final Identifier WARNING_TEXTURE =
            Identifier.fromNamespaceAndPath("creeper-alarm", "textures/hud/warning.png");
    private static final Identifier ALARM_SOUND_ID =
            Identifier.fromNamespaceAndPath("creeper-alarm", "alarm");
    private static final SoundEvent ALARM_SOUND =
            SoundEvent.createVariableRangeEvent(ALARM_SOUND_ID);
    private static final float FADE_SPEED = 0.05f;
    private static final int SOUND_COOLDOWN_TICKS = 100;

    private boolean creeperTargeting = false;
    private boolean wasCreeperTargeting = false;
    private float overlayAlpha = 0.0f;
    private int soundCooldown = 0;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(CreeperAlarmPayload.TYPE, (payload, context) -> {
            creeperTargeting = payload.creeperTargeting();

            if (creeperTargeting && !wasCreeperTargeting && soundCooldown <= 0) {
                Minecraft client = context.client();
                if (client.player != null) {
                    client.player.playSound(ALARM_SOUND, 0.5f, 1.0f);
                    soundCooldown = SOUND_COOLDOWN_TICKS;
                }
            }
            wasCreeperTargeting = creeperTargeting;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (soundCooldown > 0) soundCooldown--;
            if (creeperTargeting) {
                overlayAlpha = Math.min(1.0f, overlayAlpha + FADE_SPEED);
            } else {
                overlayAlpha = Math.max(0.0f, overlayAlpha - FADE_SPEED);
            }
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("creeper-alarm", "warning"), this);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor drawContext, DeltaTracker tickCounter) { //mojang im coming for you
        if (overlayAlpha <= 0.0f) return;

        Minecraft client = Minecraft.getInstance();
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        int alpha = (int) (overlayAlpha * 255);
        int color = (alpha << 24) | 0xFFFFFF;

        drawContext.blit(RenderPipelines.GUI_TEXTURED,
                WARNING_TEXTURE, 0, 0, 0.0f, 0.0f, screenWidth, screenHeight, screenWidth, screenHeight, color);
    }
}
