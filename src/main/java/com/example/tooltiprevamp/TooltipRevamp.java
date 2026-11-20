package com.example.tooltiprevamp;

import com.example.tooltiprevamp.client.ClientSetup;
import com.example.tooltiprevamp.network.Network;
import com.example.tooltiprevamp.server.DiscoveryEvents;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(TooltipRevamp.MODID)
public class TooltipRevamp {
    public static final String MODID = "tooltiprevamp";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TooltipRevamp(IEventBus modBus, ModContainer container) {
        modBus.addListener(Network::register);
        DiscoveryEvents.register(NeoForge.EVENT_BUS);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init(modBus, NeoForge.EVENT_BUS);
        }
    }
}
