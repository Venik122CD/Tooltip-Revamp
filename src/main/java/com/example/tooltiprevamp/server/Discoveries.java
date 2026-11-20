package com.example.tooltiprevamp.server;

import com.example.tooltiprevamp.data.ActionRegistry;
import com.example.tooltiprevamp.data.ItemAction;
import com.example.tooltiprevamp.network.DiscoveredPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Хранит открытые действия в persistent data игрока и синхронизирует их с клиентом. */
public final class Discoveries {
    public static final String KEY = "tooltiprevamp_discovered";

    private Discoveries() {}

    public static Set<ResourceLocation> get(Player player) {
        Set<ResourceLocation> out = new HashSet<>();
        ListTag list = player.getPersistentData().getList(KEY, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) out.add(id);
        }
        return out;
    }

    public static boolean has(Player player, ResourceLocation id) {
        ListTag list = player.getPersistentData().getList(KEY, Tag.TAG_STRING);
        String s = id.toString();
        for (int i = 0; i < list.size(); i++) {
            if (list.getString(i).equals(s)) return true;
        }
        return false;
    }

    public static void discover(ServerPlayer player, ItemAction action) {
        if (has(player, action.id())) return;
        CompoundTag data = player.getPersistentData();
        ListTag list = data.getList(KEY, Tag.TAG_STRING);
        list.add(StringTag.valueOf(action.id().toString()));
        data.put(KEY, list);

        sync(player);
        player.displayClientMessage(Component.translatable("tooltiprevamp.message.discovered"), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.5F);
    }

    public static void revealAll(ServerPlayer player) {
        ListTag list = new ListTag();
        for (ItemAction a : ActionRegistry.SERVER.all()) {
            list.add(StringTag.valueOf(a.id().toString()));
        }
        player.getPersistentData().put(KEY, list);
        sync(player);
    }

    public static void reset(ServerPlayer player) {
        player.getPersistentData().remove(KEY);
        sync(player);
    }

    public static void copy(Player from, Player to) {
        Tag tag = from.getPersistentData().get(KEY);
        if (tag != null) {
            to.getPersistentData().put(KEY, tag.copy());
        }
    }

    public static void sync(ServerPlayer player) {
        List<ResourceLocation> ids = new ArrayList<>(get(player));
        PacketDistributor.sendToPlayer(player, new DiscoveredPayload(ids));
    }
}
