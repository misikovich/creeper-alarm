package gurtletirl.squirt;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class CreeperAlarm implements ClientModInitializer {

    private static final Identifier WARNING_TEXTURE =
            Identifier.of("creeper-alarm", "textures/hud/warning.png");
    private static final Identifier ALARM_SOUND_ID =
            Identifier.of("creeper-alarm", "alarm");
    private static final SoundEvent ALARM_SOUND =
            SoundEvent.of(ALARM_SOUND_ID);
    private static final float FADE_SPEED = 0.05f; // ~1 second to fully fade in/out
    private static final int SOUND_COOLDOWN_TICKS = 100; // ~5 seconds

    private boolean creeperTargeting = false;
    private boolean wasCreeperTargeting = false;
    private float overlayAlpha = 0.0f;
    private int soundCooldown = 0;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(CreeperAlarmPayload.ID, (payload, context) -> {
            creeperTargeting = payload.creeperTargeting();

            if (creeperTargeting && !wasCreeperTargeting && soundCooldown <= 0) {
                MinecraftClient client = context.client();
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

        HudRenderCallback.EVENT.register(this::onHudRender);
    }

    private void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        if (overlayAlpha <= 0.0f) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, overlayAlpha);
        drawContext.drawTexture(WARNING_TEXTURE, 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
}
