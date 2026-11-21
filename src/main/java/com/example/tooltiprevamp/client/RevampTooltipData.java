package com.example.tooltiprevamp.client;

import com.example.tooltiprevamp.data.ItemAction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;


public record RevampTooltipData(Component title, Component description, List<Component> bodyLines,
                                List<ItemAction> actions, int maxLineWidth, boolean alone)
        implements TooltipComponent {
}
