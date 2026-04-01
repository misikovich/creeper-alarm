package gurtletirl.squirt;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreeperAlarmServer implements ModInitializer {
    private final Map<UUID, Boolean> previousState = new HashMap<>();

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(CreeperAlarmPayload.ID, CreeperAlarmPayload.CODEC);

        ServerTickEvents.END_WORLD_TICK.register(this::onWorldTick);
    }

    private void onWorldTick(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            boolean targeted = !world.getEntitiesByClass(
                    CreeperEntity.class,
                    player.getBoundingBox().expand(32),
                    creeper -> creeper.getTarget() == player
            ).isEmpty();

            Boolean wasTargeted = previousState.get(player.getUuid());
            if (wasTargeted == null || targeted != wasTargeted) {
                ServerPlayNetworking.send(player, new CreeperAlarmPayload(targeted));
                previousState.put(player.getUuid(), targeted);
            }
        }
    }
}
