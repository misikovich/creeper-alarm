package gurtletirl.squirt;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CreeperAlarmPayload(boolean creeperTargeting) implements CustomPayload {
    public static final Id<CreeperAlarmPayload> ID =
            new Id<>(Identifier.of("creeper-alarm", "alert"));
    public static final PacketCodec<RegistryByteBuf, CreeperAlarmPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.BOOL, CreeperAlarmPayload::creeperTargeting, CreeperAlarmPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
