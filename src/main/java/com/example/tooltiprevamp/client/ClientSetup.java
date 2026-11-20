package com.example.tooltiprevamp.client;

import com.example.tooltiprevamp.data.ActionRegistry;
import com.example.tooltiprevamp.data.ClientDiscoveries;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;

public final class ClientSetup {
    private ClientSetup() {}

    public static void init(IEventBus modBus, IEventBus gameBus) {
        modBus.addListener(ClientSetup::registerTooltipFactories);

        gameBus.addListener(EventPriority.LOW, TooltipEvents::onGather);
        gameBus.addListener(TooltipEvents::onColor);
        gameBus.addListener(ClientSetup::onLogout);
    }

    private static void registerTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(RevampTooltipData.class, RevampClientTooltip::new);
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientDiscoveries.clear();
        ActionRegistry.CLIENT.clear();
    }
}
