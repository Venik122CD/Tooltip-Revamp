package com.example.tooltiprevamp.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;


public record EntityMatcher(boolean tag, ResourceLocation id) {

    public static EntityMatcher parse(String value) {
        boolean tag = value.startsWith("#");
        return new EntityMatcher(tag, ResourceLocation.parse(tag ? value.substring(1) : value));
    }

    public boolean test(Entity entity) {
        EntityType<?> type = entity.getType();
        return tag
                ? type.is(TagKey.create(Registries.ENTITY_TYPE, id))
                : BuiltInRegistries.ENTITY_TYPE.getKey(type).equals(id);
    }
}
