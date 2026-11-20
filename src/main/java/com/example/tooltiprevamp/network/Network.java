package com.example.tooltiprevamp.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class Network {
    private Network() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(ActionsPayload.TYPE, ActionsPayload.CODEC, ActionsPayload::handle);
        registrar.playToClient(DiscoveredPayload.TYPE, DiscoveredPayload.CODEC, DiscoveredPayload::handle);
    }
}
