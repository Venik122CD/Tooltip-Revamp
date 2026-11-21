package com.example.tooltiprevamp.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;


public record ItemMatcher(boolean tag, ResourceLocation id, ResourceLocation potion) {

    public static ItemMatcher parse(JsonElement element) {
        String item;
        String potion = null;
        if (element.isJsonPrimitive()) {
            item = element.getAsString();
        } else {
            JsonObject obj = element.getAsJsonObject();
            item = GsonHelper.getAsString(obj, "item");
            potion = GsonHelper.getAsString(obj, "potion", null);
        }
        boolean tag = item.startsWith("#");
        ResourceLocation id = ResourceLocation.parse(tag ? item.substring(1) : item);
        return new ItemMatcher(tag, id, potion == null ? null : ResourceLocation.parse(potion));
    }

    public boolean test(ItemStack stack) {
        boolean ok = tag
                ? stack.is(TagKey.create(Registries.ITEM, id))
                : BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id);
        if (!ok) return false;
        if (potion == null) return true;
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.potion().map(h -> h.is(potion)).orElse(false);
    }
}
