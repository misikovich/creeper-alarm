package gurtletirl.squirt;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CreeperAlarmPayload(boolean creeperTargeting) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CreeperAlarmPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("creeper-alarm", "alert"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CreeperAlarmPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, CreeperAlarmPayload::creeperTargeting, CreeperAlarmPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
