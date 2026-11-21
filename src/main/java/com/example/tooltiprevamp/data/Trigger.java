package com.example.tooltiprevamp.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Trigger {

    public enum Type {
        BONEMEAL,
        USE_ON_BLOCK,
        ITEM_ABILITY,
        DISABLE_SHIELD,
        CRAFT,
        SMELT,
        BREW,
        CONSUME,
        USE,
        USE_ON_ENTITY,
        ATTACK,
        INTERACT_BLOCK,
        PLACE_BLOCK,
        BREAK_BLOCK,
        EQUIPPED,
        ANVIL
    }

    private final Type type;
    private final List<BlockMatcher> blocks = new ArrayList<>();
    private final List<ItemMatcher> results = new ArrayList<>();
    private final List<ResourceLocation> potions = new ArrayList<>();
    private final List<EntityMatcher> entities = new ArrayList<>();
    private final List<EquipmentSlot> slots = new ArrayList<>();
    private ItemAbility ability;
    private boolean ingredient;
    private boolean held;
    private boolean anyTool;
    private double radius = 16.0;
    private String side = "left";

    private Trigger(Type type) {
        this.type = type;
    }

    public Type type() {
        return type;
    }

    public boolean ingredient() {
        return ingredient;
    }

    public boolean held() {
        return held;
    }

    public boolean anyTool() {
        return anyTool;
    }

    public boolean rightSide() {
        return "right".equals(side);
    }

    public static Trigger parse(JsonObject json) {
        String name = GsonHelper.getAsString(json, "type");
        Type type;
        try {
            type = Type.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new JsonSyntaxException("Unknown trigger type: " + name);
        }

        Trigger t = new Trigger(type);
        if (json.has("blocks")) {
            for (JsonElement e : GsonHelper.getAsJsonArray(json, "blocks")) t.blocks.add(BlockMatcher.parse(e.getAsString()));
        }
        if (json.has("results")) {
            for (JsonElement e : GsonHelper.getAsJsonArray(json, "results")) t.results.add(ItemMatcher.parse(e));
        }
        if (json.has("potions")) {
            for (JsonElement e : GsonHelper.getAsJsonArray(json, "potions")) t.potions.add(ResourceLocation.parse(e.getAsString()));
        }
        if (json.has("entities")) {
            for (JsonElement e : GsonHelper.getAsJsonArray(json, "entities")) t.entities.add(EntityMatcher.parse(e.getAsString()));
        }
        if (type == Type.ITEM_ABILITY || json.has("ability")) {
            t.ability = ItemAbility.get(GsonHelper.getAsString(json, "ability"));
        }
        t.ingredient = GsonHelper.getAsBoolean(json, "ingredient", false);
        t.held = GsonHelper.getAsBoolean(json, "held", false);
        t.anyTool = GsonHelper.getAsBoolean(json, "any_tool", false);
        t.radius = GsonHelper.getAsFloat(json, "radius", 16.0F);
        t.side = GsonHelper.getAsString(json, "side", "left");

        String slot = GsonHelper.getAsString(json, "slot", "hands");
        switch (slot) {
            case "armor" -> t.slots.addAll(List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET));
            case "head" -> t.slots.add(EquipmentSlot.HEAD);
            case "chest" -> t.slots.add(EquipmentSlot.CHEST);
            case "legs" -> t.slots.add(EquipmentSlot.LEGS);
            case "feet" -> t.slots.add(EquipmentSlot.FEET);
            case "mainhand" -> t.slots.add(EquipmentSlot.MAINHAND);
            case "offhand" -> t.slots.add(EquipmentSlot.OFFHAND);
            default -> t.slots.addAll(List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));
        }
        return t;
    }

    public boolean blockMatches(BlockState state) {
        if (blocks.isEmpty()) return true;
        for (BlockMatcher m : blocks) {
            if (m.test(state)) return true;
        }
        return false;
    }

    public boolean matchesResult(ItemStack stack) {
        if (results.isEmpty()) return true;
        for (ItemMatcher m : results) {
            if (m.test(stack)) return true;
        }
        return false;
    }

    public boolean matchesPotion(ItemStack stack) {
        if (potions.isEmpty()) return true;
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null || contents.potion().isEmpty()) return false;
        for (ResourceLocation id : potions) {
            if (contents.potion().get().is(id)) return true;
        }
        return false;
    }

    public boolean matchesEntity(Entity entity) {
        if (entities.isEmpty()) return true;
        for (EntityMatcher m : entities) {
            if (m.test(entity)) return true;
        }
        return false;
    }

    public boolean matchesBlockUse(ServerPlayer player, InteractionHand hand, BlockHitResult hit,
                                   ItemStack stack, Level level, BlockPos pos, BlockState state) {
        if (!blockMatches(state)) return false;
        return switch (type) {
            case USE_ON_BLOCK -> true;
            case BONEMEAL -> state.getBlock() instanceof BonemealableBlock b
                    && b.isValidBonemealTarget(level, pos, state);
            case ITEM_ABILITY -> abilityWorks(player, hand, hit, stack, state);
            default -> false;
        };
    }

    public boolean matchesInteract(ServerPlayer player, InteractionHand hand, BlockHitResult hit,
                                   ItemStack heldStack, BlockState state) {
        if (!blockMatches(state)) return false;
        return ability == null || abilityWorks(player, hand, hit, heldStack, state);
    }

    private boolean abilityWorks(ServerPlayer player, InteractionHand hand, BlockHitResult hit,
                                 ItemStack stack, BlockState state) {
        return ability != null
                && !stack.isEmpty()
                && stack.canPerformAction(ability)
                && state.getToolModifiedState(new UseOnContext(player, hand, hit), ability, true) != null;
    }

    public boolean matchesEquipped(ServerPlayer player, ItemAction action) {
        boolean worn = false;
        for (EquipmentSlot slot : slots) {
            ItemStack st = player.getItemBySlot(slot);
            if (!st.isEmpty() && action.appliesTo(st)) {
                worn = true;
                break;
            }
        }
        if (!worn) return false;
        if (entities.isEmpty()) return true;

        AABB box = player.getBoundingBox().inflate(radius);
        return !player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && matchesEntity(e)).isEmpty();
    }
}