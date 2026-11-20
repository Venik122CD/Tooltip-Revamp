package com.example.tooltiprevamp.data;

import com.example.tooltiprevamp.TooltipRevamp;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

/** Читает data/<namespace>/item_actions/**.json */
public class ActionLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();

    public ActionLoader() {
        super(GSON, "item_actions");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, ItemAction> parsed = new HashMap<>();
        Map<ResourceLocation, String> raw = new HashMap<>();

        objects.forEach((id, element) -> {
            try {
                parsed.put(id, ItemAction.parse(id, GsonHelper.convertToJsonObject(element, "item action")));
                raw.put(id, GSON.toJson(element));
            } catch (Exception ex) {
                TooltipRevamp.LOGGER.error("Failed to load item action {}", id, ex);
            }
        });

        ActionRegistry.SERVER.set(parsed, raw);
        TooltipRevamp.LOGGER.info("Loaded {} item actions", parsed.size());
    }
}
