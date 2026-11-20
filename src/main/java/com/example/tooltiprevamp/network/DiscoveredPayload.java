package com.example.tooltiprevamp.network;

import com.example.tooltiprevamp.TooltipRevamp;
import com.example.tooltiprevamp.data.ClientDiscoveries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record DiscoveredPayload(List<ResourceLocation> ids) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DiscoveredPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TooltipRevamp.MODID, "discovered"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiscoveredPayload> CODEC =
            StreamCodec.of(DiscoveredPayload::write, DiscoveredPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, DiscoveredPayload payload) {
        buf.writeVarInt(payload.ids.size());
        for (ResourceLocation id : payload.ids) {
            buf.writeResourceLocation(id);
        }
    }

    private static DiscoveredPayload read(RegistryFriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<ResourceLocation> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(buf.readResourceLocation());
        }
        return new DiscoveredPayload(list);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DiscoveredPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientDiscoveries.set(payload.ids));
    }
}
