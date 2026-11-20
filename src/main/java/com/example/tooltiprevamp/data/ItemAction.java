package com.example.tooltiprevamp.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record ItemAction(ResourceLocation id, List<ItemMatcher> items, String text, int order, Trigger trigger) {

    public static ItemAction parse(ResourceLocation id, JsonObject json) {
        List<ItemMatcher> items = new ArrayList<>();
        for (JsonElement e : GsonHelper.getAsJsonArray(json, "items")) {
            items.add(ItemMatcher.parse(e));
        }
        String text = GsonHelper.getAsString(json, "text");
        int order = GsonHelper.getAsInt(json, "order", 0);
        Trigger trigger = Trigger.parse(GsonHelper.getAsJsonObject(json, "trigger"));
        return new ItemAction(id, items, text, order, trigger);
    }

    public boolean appliesTo(ItemStack stack) {
        for (ItemMatcher m : items) {
            if (m.test(stack)) return true;
        }
        return false;
    }
}
