package com.example.tooltiprevamp.network;

import com.example.tooltiprevamp.TooltipRevamp;
import com.example.tooltiprevamp.data.ActionRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;


public record ActionsPayload(Map<ResourceLocation, String> actions) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ActionsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TooltipRevamp.MODID, "actions"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ActionsPayload> CODEC =
            StreamCodec.of(ActionsPayload::write, ActionsPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, ActionsPayload payload) {
        buf.writeVarInt(payload.actions.size());
        payload.actions.forEach((id, json) -> {
            buf.writeResourceLocation(id);
            buf.writeUtf(json, 32767);
        });
    }

    private static ActionsPayload read(RegistryFriendlyByteBuf buf) {
        int n = buf.readVarInt();
        Map<ResourceLocation, String> map = new HashMap<>();
        for (int i = 0; i < n; i++) {
            ResourceLocation id = buf.readResourceLocation();
            map.put(id, buf.readUtf(32767));
        }
        return new ActionsPayload(map);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ActionsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ActionRegistry.CLIENT.setFromSync(payload.actions));
    }
}
