package com.asted.tmails;

import net.minecraftforge.common.ForgeConfigSpec;

public class AllConfigs
{
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Boolean> AUTO_TRANSLATE;

    static {
        BUILDER.push("Configs for TMails");

        // Configs
        AUTO_TRANSLATE = BUILDER.comment("Should all mails be translated automatically upon new page?").define("Auto Translate", true);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
