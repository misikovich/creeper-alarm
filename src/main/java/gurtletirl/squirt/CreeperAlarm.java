package gurtletirl.squirt;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreeperAlarm implements ClientModInitializer, HudElement {

    private static final Logger LOGGER = LoggerFactory.getLogger("creeper-alarm");

    private static final Identifier WARNING_TEXTURE = Identifier.fromNamespaceAndPath("creeper-alarm", "textures/hud/warning.png");
    private static final SoundEvent ALARM_SOUND = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("creeper-alarm", "alarm"));

    private static final int    DETECTION_INTERVAL_TICK = 5;
    private static final double DETECTION_RADIUS_BLOCKS = 10.0;
    private static final double DETECTION_FACING_THRESHOLD_DEG = 45.0; // ±45° yaw cone
    private static final float  TEXTURE_FADE_SPEED = 0.05f;
    private static final float  SOUND_VOLUME = 0.7f;
    private static final float  SOUND_PITCH = 1.0f;

    private boolean isCreeperTargeting = false;
    private boolean wasCreeperTargeting = false;
    private float   overlayAlpha = 0.0f;
    private int     tickCounter = 0;
    private LoopingAlarmSound activeSound = null;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (++tickCounter % DETECTION_INTERVAL_TICK != 0) {
                if (isCreeperTargeting) {
                    overlayAlpha = Math.min(1.0f, overlayAlpha + TEXTURE_FADE_SPEED);
                } else {
                    overlayAlpha = Math.max(0.0f, overlayAlpha - TEXTURE_FADE_SPEED);
                }
                return;
            }

            LocalPlayer player = client.player;
            if (player == null || client.level == null) {
                isCreeperTargeting = false;
            } else {
                AABB searchBox = player.getBoundingBox().inflate(DETECTION_RADIUS_BLOCKS);
                isCreeperTargeting = !client.level.getEntities(
                        EntityTypeTest.forClass(Creeper.class),
                        searchBox,
                        creeper -> isMovingTowardPlayer(creeper, player)
                ).isEmpty();
            }

            if (isCreeperTargeting != wasCreeperTargeting) {
                SoundManager sound_mgr = Minecraft.getInstance().getSoundManager();
                LOGGER.debug("Creeper targeting state changed: {}", isCreeperTargeting);
                if (isCreeperTargeting) {
                    activeSound = new LoopingAlarmSound(ALARM_SOUND);
                    sound_mgr.play(activeSound);
                    LOGGER.debug("Alarm triggered, playing looping sound");
                } else if (activeSound != null) {
                    sound_mgr.stop(activeSound);
                    activeSound = null;
                    LOGGER.debug("Alarm cleared, stopping sound");
                }
            }
            wasCreeperTargeting = isCreeperTargeting;

            if (isCreeperTargeting) {
                overlayAlpha = Math.min(1.0f, overlayAlpha + TEXTURE_FADE_SPEED);
            } else {
                overlayAlpha = Math.max(0.0f, overlayAlpha - TEXTURE_FADE_SPEED);
            }
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("creeper-alarm", "warning"), this);
    }


    private boolean isMovingTowardPlayer(Creeper creeper, LocalPlayer player) {
        Vec3 toPlayer = player.position().subtract(creeper.position());
        double angleToPlayer = Math.toDegrees(Math.atan2(-toPlayer.x, toPlayer.z));
        double yawDiff = Math.abs(((creeper.getYRot() - angleToPlayer) % 360 + 540) % 360 - 180);
        double dist = creeper.position().distanceTo(player.position());
        LOGGER.debug("Creeper {} | dist={} | yawDiff={} | threshold={} | facing={}",
                creeper.getId(), String.format("%.2f", dist), String.format("%.1f", yawDiff),
                DETECTION_FACING_THRESHOLD_DEG, yawDiff < DETECTION_FACING_THRESHOLD_DEG);
        return yawDiff < DETECTION_FACING_THRESHOLD_DEG;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor drawContext, @NonNull DeltaTracker tickCounter) {
        if (overlayAlpha <= 0.0f) return;

        Minecraft client = Minecraft.getInstance();
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        int alpha = (int) (overlayAlpha * 255);
        int color = (alpha << 24) | 0xFFFFFF;

        drawContext.blit(
                RenderPipelines.GUI_TEXTURED,
                WARNING_TEXTURE,
                0, 0, 0.0f, 0.0f,
                screenWidth,
                screenHeight,
                screenWidth,
                screenHeight,
                color
        );
    }

    private static class LoopingAlarmSound extends AbstractTickableSoundInstance {
        LoopingAlarmSound(SoundEvent sound) {
            super(sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = SOUND_VOLUME;
            this.pitch = SOUND_PITCH;
        }

        @Override
        public void tick() {}
    }
}
