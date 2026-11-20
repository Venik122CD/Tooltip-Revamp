package com.example.tooltiprevamp.server;

import com.example.tooltiprevamp.data.ActionLoader;
import com.example.tooltiprevamp.data.ActionRegistry;
import com.example.tooltiprevamp.data.ItemAction;
import com.example.tooltiprevamp.data.Trigger;
import com.example.tooltiprevamp.network.ActionsPayload;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public final class DiscoveryEvents {
    private DiscoveryEvents() {}

    public static void register(IEventBus bus) {
        bus.addListener(DiscoveryEvents::onAddReloadListeners);
        bus.addListener(DiscoveryEvents::onDatapackSync);
        bus.addListener(DiscoveryEvents::onLogin);
        bus.addListener(DiscoveryEvents::onRespawn);
        bus.addListener(DiscoveryEvents::onClone);
        bus.addListener(DiscoveryEvents::onRegisterCommands);
        bus.addListener(EventPriority.LOWEST, DiscoveryEvents::onRightClickBlock);
        bus.addListener(DiscoveryEvents::onIncomingDamage);
    }

    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ActionLoader());
    }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        ActionsPayload payload = new ActionsPayload(ActionRegistry.SERVER.raw());
        event.getRelevantPlayers().forEach(player -> {
            PacketDistributor.sendToPlayer(player, payload);
            Discoveries.sync(player);
        });
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new ActionsPayload(ActionRegistry.SERVER.raw()));
            Discoveries.sync(player);
        }
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Discoveries.sync(player);
        }
    }

    private static void onClone(PlayerEvent.Clone event) {
        Discoveries.copy(event.getOriginal(), event.getEntity());
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        List<ItemAction> candidates = ActionRegistry.SERVER.forStack(stack);
        if (candidates.isEmpty()) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        for (ItemAction action : candidates) {
            if (Discoveries.has(player, action.id())) continue;
            if (action.trigger().matchesBlockUse(player, event.getHand(), event.getHitVec(), stack, level, pos, state)) {
                Discoveries.discover(player, action);
            }
        }
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;

        LivingEntity victim = event.getEntity();
        if (!victim.isBlocking()) return;

        ItemStack weapon = attacker.getMainHandItem();
        List<ItemAction> candidates = ActionRegistry.SERVER.forStack(weapon);
        if (candidates.isEmpty()) return;

        for (ItemAction action : candidates) {
            if (action.trigger().type() != Trigger.Type.DISABLE_SHIELD) continue;
            if (Discoveries.has(attacker, action.id())) continue;
            if (weapon.canDisableShield(victim.getUseItem(), victim, attacker)) {
                Discoveries.discover(attacker, action);
            }
        }
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tooltiprevamp")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("reset").executes(ctx -> {
                    Discoveries.reset(ctx.getSource().getPlayerOrException());
                    ctx.getSource().sendSuccess(() -> Component.literal("Discoveries reset"), false);
                    return 1;
                }))
                .then(Commands.literal("reveal_all").executes(ctx -> {
                    Discoveries.revealAll(ctx.getSource().getPlayerOrException());
                    ctx.getSource().sendSuccess(() -> Component.literal("All actions discovered"), false);
                    return 1;
                })));
    }
}
