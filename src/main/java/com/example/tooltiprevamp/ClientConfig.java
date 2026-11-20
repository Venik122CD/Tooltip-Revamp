package com.example.tooltiprevamp;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        ENABLED = builder
                .comment("Включить новый стиль тултипов (чёрный фон, центрирование, страница действий по Ctrl)")
                .define("enabled", true);
        SPEC = builder.build();
    }

    private ClientConfig() {}
}
