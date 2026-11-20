package com.example.tooltiprevamp.data;

import com.example.tooltiprevamp.TooltipRevamp;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ActionRegistry {
    public static final ActionRegistry SERVER = new ActionRegistry();
    public static final ActionRegistry CLIENT = new ActionRegistry();

    private static final Comparator<ItemAction> ORDER =
            Comparator.comparingInt(ItemAction::order).thenComparing(ItemAction::id);

    private volatile List<ItemAction> actions = List.of();
    private volatile Map<ResourceLocation, String> rawJson = Map.of();

    private ActionRegistry() {}

    public void set(Map<ResourceLocation, ItemAction> parsed, Map<ResourceLocation, String> raw) {
        actions = parsed.values().stream().sorted(ORDER).toList();
        rawJson = Map.copyOf(raw);
    }

    public void setFromSync(Map<ResourceLocation, String> raw) {
        Map<ResourceLocation, ItemAction> parsed = new HashMap<>();
        for (Map.Entry<ResourceLocation, String> e : raw.entrySet()) {
            try {
                parsed.put(e.getKey(), ItemAction.parse(e.getKey(), JsonParser.parseString(e.getValue()).getAsJsonObject()));
            } catch (Exception ex) {
                TooltipRevamp.LOGGER.error("Bad synced action {}", e.getKey(), ex);
            }
        }
        set(parsed, raw);
        TooltipRevamp.LOGGER.info("Client received {} item actions", parsed.size());
    }

    public void clear() {
        actions = List.of();
        rawJson = Map.of();
    }

    public Map<ResourceLocation, String> raw() {
        return rawJson;
    }

    public List<ItemAction> all() {
        return actions;
    }

    public List<ItemAction> forStack(ItemStack stack) {
        if (stack.isEmpty()) return List.of();
        List<ItemAction> out = null;
        for (ItemAction a : actions) {
            if (a.appliesTo(stack)) {
                if (out == null) out = new ArrayList<>();
                out.add(a);
            }
        }
        return out == null ? List.of() : out;
    }
}
