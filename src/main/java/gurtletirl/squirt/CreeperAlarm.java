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
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class CreeperAlarm implements ClientModInitializer, HudElement {

    private static final Logger LOGGER = LoggerFactory.getLogger("creeper-alarm");

    private static final Identifier[] WARNING_TEXTURES = {
            Identifier.fromNamespaceAndPath("creeper-alarm", "textures/hud/warning-ru.png"),
            Identifier.fromNamespaceAndPath("creeper-alarm", "textures/hud/warning-ru-sob.png"),
    };
    private static final SoundEvent[] ALARM_SOUNDS = {
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("creeper-alarm", "alarm-real"))
    };
    private static final float FADE_SPEED = 0.05f;
    private static final double DETECTION_RADIUS = 10.0;

    private static final int DETECTION_INTERVAL = 5;

    private boolean creeperTargeting = false;
    private boolean wasCreeperTargeting = false;
    private float overlayAlpha = 0.0f;
    private int tickCounter = 0;
    private LoopingAlarmSound activeSound = null;
    private Identifier activeTexture = WARNING_TEXTURES[0];

    private final Random RANDOM = new Random();

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (++tickCounter % DETECTION_INTERVAL != 0) {
                if (creeperTargeting) {
                    overlayAlpha = Math.min(1.0f, overlayAlpha + FADE_SPEED);
                } else {
                    overlayAlpha = Math.max(0.0f, overlayAlpha - FADE_SPEED);
                }
                return;
            }

            LocalPlayer player = client.player;
            if (player == null || client.level == null) {
                creeperTargeting = false;
            } else {
                AABB searchBox = player.getBoundingBox().inflate(DETECTION_RADIUS);
                creeperTargeting = !client.level.getEntities(
                        EntityTypeTest.forClass(Creeper.class),
                        searchBox,
                        creeper -> isMovingTowardPlayer(creeper, player)
                ).isEmpty();
            }

            if (creeperTargeting != wasCreeperTargeting) {
                LOGGER.debug("Creeper targeting state changed: {}", creeperTargeting);
                if (creeperTargeting) {
                    activeTexture = getRandomId(WARNING_TEXTURES);
                    activeSound = new LoopingAlarmSound(getRandomSound(ALARM_SOUNDS));
                    Minecraft.getInstance().getSoundManager().play(activeSound);
                    LOGGER.debug("Alarm triggered, playing looping sound");
                } else if (activeSound != null) {
                    Minecraft.getInstance().getSoundManager().stop(activeSound);
                    activeSound = null;
                    LOGGER.debug("Alarm cleared, stopping sound");
                }
            }
            wasCreeperTargeting = creeperTargeting;

            if (creeperTargeting) {
                overlayAlpha = Math.min(1.0f, overlayAlpha + FADE_SPEED);
            } else {
                overlayAlpha = Math.max(0.0f, overlayAlpha - FADE_SPEED);
            }
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("creeper-alarm", "warning"), this);
    }

    private static final double FACING_THRESHOLD_DEG = 45.0; // ±45° yaw cone

    private boolean isMovingTowardPlayer(Creeper creeper, LocalPlayer player) {
        Vec3 toPlayer = player.position().subtract(creeper.position());
        double angleToPlayer = Math.toDegrees(Math.atan2(-toPlayer.x, toPlayer.z));
        double yawDiff = Math.abs(((creeper.getYRot() - angleToPlayer) % 360 + 540) % 360 - 180);
        double dist = creeper.position().distanceTo(player.position());
        LOGGER.debug("Creeper {} | dist={} | yawDiff={} | threshold={} | facing={}",
                creeper.getId(), String.format("%.2f", dist), String.format("%.1f", yawDiff),
                FACING_THRESHOLD_DEG, yawDiff < FACING_THRESHOLD_DEG);
        return yawDiff < FACING_THRESHOLD_DEG;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor drawContext, DeltaTracker tickCounter) {
        if (overlayAlpha <= 0.0f) return;

        Minecraft client = Minecraft.getInstance();
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        int alpha = (int) (overlayAlpha * 255);
        int color = (alpha << 24) | 0xFFFFFF;

        drawContext.blit(RenderPipelines.GUI_TEXTURED,
                activeTexture, 0, 0, 0.0f, 0.0f, screenWidth, screenHeight, screenWidth, screenHeight, color);
    }

    Identifier getRandomId(Identifier[] ids) {
        int randID = RANDOM.nextInt(ids.length);
        return ids[randID];
    }

    SoundEvent getRandomSound(SoundEvent[] sounds) {
        int randSound = RANDOM.nextInt(sounds.length);
        return sounds[randSound];
    }

    private static class LoopingAlarmSound extends AbstractTickableSoundInstance {
        LoopingAlarmSound(SoundEvent sound) {
            super(sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = 0.5f;
            this.pitch = 1.0f;
        }

        @Override
        public void tick() {}
    }
}
