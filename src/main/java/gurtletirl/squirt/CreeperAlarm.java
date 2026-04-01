package gurtletirl.squirt;

import net.fabricmc.api.ClientModInitializer;
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

    private boolean creeperTargeting = false;
    private boolean wasCreeperTargeting = false;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(CreeperAlarmPayload.ID, (payload, context) -> {
            creeperTargeting = payload.creeperTargeting();

            if (creeperTargeting && !wasCreeperTargeting) {
                MinecraftClient client = context.client();
                if (client.player != null) {
                    client.player.playSound(ALARM_SOUND, 1.0f, 1.0f);
                }
            }
            wasCreeperTargeting = creeperTargeting;
        });

        HudRenderCallback.EVENT.register(this::onHudRender);
    }

    private void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        if (!creeperTargeting) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        drawContext.drawTexture(WARNING_TEXTURE, 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);
    }
}
