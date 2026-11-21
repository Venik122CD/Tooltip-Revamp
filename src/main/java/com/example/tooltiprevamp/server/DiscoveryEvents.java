package com.example.tooltiprevamp.server;

import com.example.tooltiprevamp.data.ActionLoader;
import com.example.tooltiprevamp.data.ActionRegistry;
import com.example.tooltiprevamp.data.ItemAction;
import com.example.tooltiprevamp.data.Trigger;
import com.example.tooltiprevamp.network.ActionsPayload;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AnvilRepairEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;


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
        bus.addListener(DiscoveryEvents::onRightClickItem);
        bus.addListener(DiscoveryEvents::onEntityInteract);
        bus.addListener(DiscoveryEvents::onIncomingDamage);
        bus.addListener(DiscoveryEvents::onCrafted);
        bus.addListener(DiscoveryEvents::onSmelted);
        bus.addListener(DiscoveryEvents::onBrewed);
        bus.addListener(DiscoveryEvents::onConsumed);
        bus.addListener(DiscoveryEvents::onAnvil);
        bus.addListener(DiscoveryEvents::onPlace);
        bus.addListener(DiscoveryEvents::onBreak);
        bus.addListener(DiscoveryEvents::onPlayerTick);
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

  
    private static void fire(ServerPlayer player, Trigger.Type type, Predicate<ItemAction> test) {
        List<ItemAction> list = ActionRegistry.SERVER.byType(type);
        if (list.isEmpty()) return;
        Set<ResourceLocation> known = Discoveries.get(player);
        for (ItemAction action : list) {
            if (known.contains(action.id())) continue;
            if (test.test(action)) Discoveries.discover(player, action);
        }
    }


    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = event.getItemStack();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        if (!stack.isEmpty()) {
            List<ItemAction> candidates = ActionRegistry.SERVER.forStack(stack);
            if (!candidates.isEmpty()) {
                Set<ResourceLocation> known = Discoveries.get(player);
                for (ItemAction action : candidates) {
                    if (known.contains(action.id())) continue;
                    if (action.trigger().matchesBlockUse(player, event.getHand(), event.getHitVec(), stack, level, pos, state)) {
                        Discoveries.discover(player, action);
                    }
                }
            }
        }

        fire(player, Trigger.Type.INTERACT_BLOCK, action -> {
            Trigger t = action.trigger();
            if (t.held() && (stack.isEmpty() || !action.appliesTo(stack))) return false;
            return t.matchesInteract(player, event.getHand(), event.getHitVec(), stack, state);
        });
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        BlockState placed = event.getPlacedBlock();
        fire(player, Trigger.Type.PLACE_BLOCK, action -> action.trigger().blockMatches(placed));
    }

    private static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        BlockState state = event.getState();
        ItemStack tool = player.getMainHandItem();
        fire(player, Trigger.Type.BREAK_BLOCK, action -> {
            Trigger t = action.trigger();
            if (!t.blockMatches(state)) return false;
            return t.anyTool() || (!tool.isEmpty() && action.appliesTo(tool));
        });
    }


    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        fire(player, Trigger.Type.USE, action -> action.appliesTo(stack));
    }

    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        Entity target = event.getTarget();
        fire(player, Trigger.Type.USE_ON_ENTITY, action -> action.appliesTo(stack) && action.trigger().matchesEntity(target));
    }

    private static void onConsumed(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem();
        if (stack.isEmpty()) return;
        fire(player, Trigger.Type.CONSUME, action -> action.appliesTo(stack));
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;

        ItemStack weapon = attacker.getMainHandItem();
        if (weapon.isEmpty()) return;

        List<ItemAction> candidates = ActionRegistry.SERVER.forStack(weapon);
        if (candidates.isEmpty()) return;

        LivingEntity victim = event.getEntity();
        Set<ResourceLocation> known = Discoveries.get(attacker);
        for (ItemAction action : candidates) {
            if (known.contains(action.id())) continue;
            Trigger t = action.trigger();
            if (t.type() == Trigger.Type.DISABLE_SHIELD) {
                if (victim.isBlocking() && weapon.canDisableShield(victim.getUseItem(), victim, attacker)) {
                    Discoveries.discover(attacker, action);
                }
            } else if (t.type() == Trigger.Type.ATTACK) {
                if (t.matchesEntity(victim)) Discoveries.discover(attacker, action);
            }
        }
    }


    private static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack result = event.getCrafting();
        Container grid = event.getInventory();
        fire(player, Trigger.Type.CRAFT, action -> {
            Trigger t = action.trigger();
            if (!t.matchesResult(result)) return false;
            if (!t.ingredient()) return true;
            for (int i = 0; i < grid.getContainerSize(); i++) {
                ItemStack s = grid.getItem(i);
                if (!s.isEmpty() && action.appliesTo(s)) return true;
            }
            return false;
        });
    }

    private static void onSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack result = event.getSmelting();
        fire(player, Trigger.Type.SMELT, action -> action.trigger().matchesResult(result));
    }

    private static void onBrewed(PlayerBrewedPotionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack potion = event.getStack();
        fire(player, Trigger.Type.BREW, action ->
                action.trigger().matchesResult(potion) && action.trigger().matchesPotion(potion));
    }

    private static void onAnvil(AnvilRepairEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        fire(player, Trigger.Type.ANVIL, action -> {
            ItemStack s = action.trigger().rightSide() ? right : left;
            return !s.isEmpty() && action.appliesTo(s);
        });
    }


    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;
        fire(player, Trigger.Type.EQUIPPED, action -> action.trigger().matchesEquipped(player, action));
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
