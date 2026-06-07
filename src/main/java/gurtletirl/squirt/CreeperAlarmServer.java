package gurtletirl.squirt;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.entity.EntityTypeTest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreeperAlarmServer implements ModInitializer {
    private final Map<UUID, Boolean> previousState = new HashMap<>();

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(CreeperAlarmPayload.TYPE, CreeperAlarmPayload.CODEC);

        ServerTickEvents.END_LEVEL_TICK.register(this::onWorldTick);
    }

    private void onWorldTick(ServerLevel world) {
        for (ServerPlayer player : world.getPlayers(p -> true)) {
            boolean targeted = !world.getEntities(
                    EntityTypeTest.forClass(Creeper.class),
                    player.getBoundingBox().inflate(32),
                    creeper -> creeper.getTarget() == player
            ).isEmpty();

            Boolean wasTargeted = previousState.get(player.getUUID());
            if (wasTargeted == null || targeted != wasTargeted) {
                ServerPlayNetworking.send(player, new CreeperAlarmPayload(targeted));
                previousState.put(player.getUUID(), targeted);
            }
        }
    }
}
