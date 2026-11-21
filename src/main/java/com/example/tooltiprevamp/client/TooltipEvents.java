package com.example.tooltiprevamp.client;

import com.example.tooltiprevamp.ClientConfig;
import com.example.tooltiprevamp.data.ActionRegistry;
import com.example.tooltiprevamp.data.ItemAction;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TooltipEvents {
    private TooltipEvents() {}

    public static void onColor(RenderTooltipEvent.Color event) {
        if (!ClientConfig.ENABLED.get() || event.getItemStack().isEmpty()) return;
        event.setBackgroundStart(RevampClientTooltip.COLOR_BLACK);
        event.setBackgroundEnd(RevampClientTooltip.COLOR_BLACK);
        event.setBorderStart(RevampClientTooltip.COLOR_BORDER);
        event.setBorderEnd(RevampClientTooltip.COLOR_BORDER);
    }

    public static void onGather(RenderTooltipEvent.GatherComponents event) {
        if (!ClientConfig.ENABLED.get()) return;

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
        if (elements.isEmpty() || elements.get(0).left().isEmpty()) return;

        Component title = toComponent(elements.get(0).left().get());
        List<Component> body = new ArrayList<>();
        List<Either<FormattedText, TooltipComponent>> others = new ArrayList<>();
        for (int i = 1; i < elements.size(); i++) {
            Either<FormattedText, TooltipComponent> e = elements.get(i);
            if (e.left().isPresent()) {
                body.add(toComponent(e.left().get()));
            } else {
                others.add(e);
            }
        }

        List<ItemAction> actions = ActionRegistry.CLIENT.forStack(stack);
        int maxLine = Math.max(120, Math.min(event.getScreenWidth() - 40, 260));
        RevampTooltipData data = new RevampTooltipData(title, descriptionFor(stack), body, actions, maxLine, others.isEmpty());

        elements.clear();
        elements.add(Either.<FormattedText, TooltipComponent>right(data));
        elements.addAll(others);
    }

    private static Component toComponent(FormattedText text) {
        return text instanceof Component c ? c : Component.literal(text.getString());
    }

    private static Component descriptionFor(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String base = "tooltiprevamp.description." + id.getNamespace() + "." + id.getPath().replace('/', '.');
        Language lang = Language.getInstance();

        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents != null) {
            Optional<ResourceLocation> potionId = contents.potion().flatMap(Holder::unwrapKey).map(ResourceKey::location);
            if (potionId.isPresent()) {
                String key = base + "." + potionId.get().getPath();
                if (lang.has(key)) return Component.translatable(key);
            }
        }
        return lang.has(base) ? Component.translatable(base) : null;
    }
}