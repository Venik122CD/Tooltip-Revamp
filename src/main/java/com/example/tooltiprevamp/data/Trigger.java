package com.example.tooltiprevamp.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Условие, при котором действие считается открытым. */
public final class Trigger {

    public enum Type {
        /** Кость/удобрение на блок, который реально можно удобрить. */
        BONEMEAL,
        /** Правый клик предметом по блоку (простая проверка). */
        USE_ON_BLOCK,
        /** Предмет умеет ItemAbility (axe_strip, axe_scrape, axe_wax_off...) и блок реально меняется. */
        ITEM_ABILITY,
        /** Удар предметом по блокирующему щитом противнику. */
        DISABLE_SHIELD
    }

    private final Type type;
    private final List<BlockMatcher> blocks;
    private final ItemAbility ability;

    private Trigger(Type type, List<BlockMatcher> blocks, ItemAbility ability) {
        this.type = type;
        this.blocks = blocks;
        this.ability = ability;
    }

    public Type type() {
        return type;
    }

    public static Trigger parse(JsonObject json) {
        String name = GsonHelper.getAsString(json, "type");
        Type type = switch (name.toLowerCase(Locale.ROOT)) {
            case "bonemeal" -> Type.BONEMEAL;
            case "use_on_block" -> Type.USE_ON_BLOCK;
            case "item_ability" -> Type.ITEM_ABILITY;
            case "disable_shield" -> Type.DISABLE_SHIELD;
            default -> throw new JsonSyntaxException("Unknown trigger type: " + name);
        };

        List<BlockMatcher> blocks = new ArrayList<>();
        if (json.has("blocks")) {
            for (JsonElement e : GsonHelper.getAsJsonArray(json, "blocks")) {
                blocks.add(BlockMatcher.parse(e.getAsString()));
            }
        }

        ItemAbility ability = null;
        if (type == Type.ITEM_ABILITY) {
            ability = ItemAbility.get(GsonHelper.getAsString(json, "ability"));
        }
        return new Trigger(type, blocks, ability);
    }

    private boolean blockMatches(BlockState state) {
        if (blocks.isEmpty()) return true;
        for (BlockMatcher m : blocks) {
            if (m.test(state)) return true;
        }
        return false;
    }

    /** Серверная проверка правого клика по блоку. */
    public boolean matchesBlockUse(ServerPlayer player, InteractionHand hand, BlockHitResult hit,
                                   ItemStack stack, Level level, BlockPos pos, BlockState state) {
        if (!blockMatches(state)) return false;
        return switch (type) {
            case USE_ON_BLOCK -> true;
            case BONEMEAL -> state.getBlock() instanceof BonemealableBlock b
                    && b.isValidBonemealTarget(level, pos, state);
            case ITEM_ABILITY -> ability != null
                    && stack.canPerformAction(ability)
                    && state.getToolModifiedState(new UseOnContext(player, hand, hit), ability, true) != null;
            case DISABLE_SHIELD -> false;
        };
    }
}
