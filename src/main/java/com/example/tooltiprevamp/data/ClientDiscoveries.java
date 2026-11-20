package com.example.tooltiprevamp.data;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Set;

public final class ClientDiscoveries {
    private static volatile Set<ResourceLocation> discovered = Set.of();

    private ClientDiscoveries() {}

    public static void set(Collection<ResourceLocation> ids) {
        discovered = Set.copyOf(ids);
    }

    public static boolean has(ResourceLocation id) {
        return discovered.contains(id);
    }

    public static void clear() {
        discovered = Set.of();
    }
}
