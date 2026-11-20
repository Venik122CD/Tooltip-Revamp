package com.example.tooltiprevamp.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;

public record BlockMatcher(boolean tag, ResourceLocation id) {

    public static BlockMatcher parse(String value) {
        boolean tag = value.startsWith("#");
        return new BlockMatcher(tag, ResourceLocation.parse(tag ? value.substring(1) : value));
    }

    public boolean test(BlockState state) {
        return tag
                ? state.is(TagKey.create(Registries.BLOCK, id))
                : BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(id);
    }
}
