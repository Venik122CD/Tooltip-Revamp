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

/** Хранилище действий. На сервере наполняется датапаками, на клиенте - пакетом синхронизации. */
public final class ActionRegistry {
    private static final Comparator<ItemAction> ORDER =
            Comparator.comparingInt(ItemAction::order).thenComparing(ItemAction::id);

    private static volatile List<ItemAction> actions = List.of();
    private static volatile Map<ResourceLocation, String> rawJson = Map.of();

    private ActionRegistry() {}

    public static void set(Map<ResourceLocation, ItemAction> parsed, Map<ResourceLocation, String> raw) {
        actions = parsed.values().stream().sorted(ORDER).toList();
        rawJson = Map.copyOf(raw);
    }

    public static void setFromSync(Map<ResourceLocation, String> raw) {
        Map<ResourceLocation, ItemAction> parsed = new HashMap<>();
        for (Map.Entry<ResourceLocation, String> e : raw.entrySet()) {
            try {
                parsed.put(e.getKey(), ItemAction.parse(e.getKey(), JsonParser.parseString(e.getValue()).getAsJsonObject()));
            } catch (Exception ex) {
                TooltipRevamp.LOGGER.error("Bad synced action {}", e.getKey(), ex);
            }
        }
        set(parsed, raw);
    }

    public static void clear() {
        actions = List.of();
        rawJson = Map.of();
    }

    public static Map<ResourceLocation, String> raw() {
        return rawJson;
    }

    public static List<ItemAction> all() {
        return actions;
    }

    public static List<ItemAction> forStack(ItemStack stack) {
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
