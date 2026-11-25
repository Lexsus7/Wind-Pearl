package com.Lezzez.client.config;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.network.chat.Component;
import java.util.Optional;

public final class WindPearlModMenu implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            var config = WindPearlConfig.INSTANCE;
            var builder = ConfigBuilder.create().setParentScreen(parent).setTitle(Component.literal("WindPearl 2.0"));
            var category = builder.getOrCreateCategory(Component.translatable("windpearl.config.general"));
            var entries = builder.entryBuilder();
            category.addEntry(entries.startStrField(Component.translatable("windpearl.config.key"), config.key)
                .setDefaultValue("G").setErrorSupplier(value -> WindPearlConfig.keyCode(value) < 0
                    ? Optional.of(Component.translatable("windpearl.config.invalid_key")) : Optional.empty())
                .setSaveConsumer(value -> config.key = value.trim().toUpperCase(java.util.Locale.ROOT)).build());
            category.addEntry(entries.startBooleanToggle(Component.translatable("windpearl.config.analysis"), config.useAnalysis)
                .setDefaultValue(true).setSaveConsumer(value -> config.useAnalysis = value).build());
            category.addEntry(entries.startBooleanToggle(Component.translatable("windpearl.config.manual"), config.manualAutoThrow)
                .setDefaultValue(true).setSaveConsumer(value -> config.manualAutoThrow = value).build());
            builder.setSavingRunnable(WindPearlConfig::save);
            return builder.build();
        };
    }
}
