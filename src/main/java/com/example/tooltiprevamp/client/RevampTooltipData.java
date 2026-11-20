package com.example.tooltiprevamp.client;

import com.example.tooltiprevamp.data.ItemAction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;

/**
 * Данные нового тултипа предмета.
 *
 * @param description описание предмета (может быть null)
 * @param bodyLines   "оригинальный" тултип: эффекты, атрибуты, зачарования и т.д.
 * @param alone       true, если кроме нашего компонента в тултипе нет других (картинки и т.п.)
 */
public record RevampTooltipData(Component title, Component description, List<Component> bodyLines,
                                List<ItemAction> actions, int maxLineWidth, boolean alone)
        implements TooltipComponent {
}
