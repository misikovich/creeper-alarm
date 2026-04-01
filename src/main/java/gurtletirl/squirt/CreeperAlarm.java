package gurtletirl.squirt;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public class CreeperAlarm implements ClientModInitializer {

    private static final Identifier WARNING_TEXTURE =
            Identifier.of("creeper-alarm", "textures/hud/warning.png");
    private static final double WARN_RANGE = 10.0;

    private boolean creeperNearby = false;
    private boolean wasCreeperNearby = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        HudRenderCallback.EVENT.register(this::onHudRender);
    }

    private void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            creeperNearby = false;
            wasCreeperNearby = false;
            return;
        }

        creeperNearby = !client.world.getEntitiesByClass(
                CreeperEntity.class,
                client.player.getBoundingBox().expand(WARN_RANGE),
                creeper -> true
        ).isEmpty();

        if (creeperNearby && !wasCreeperNearby) {
            client.player.playSound(
                    SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 1.0f
            );
        }

        wasCreeperNearby = creeperNearby;
    }

    private void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        if (!creeperNearby) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        drawContext.drawTexture(WARNING_TEXTURE, 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);
    }
}
