package com.suntide_20210418.advancedmemorycard;

import com.suntide_20210418.advancedmemorycard.datagen.ModEnusLangProvider;
import com.suntide_20210418.advancedmemorycard.datagen.ModItemModelsProvider;
import com.suntide_20210418.advancedmemorycard.datagen.ModRecipesProvider;
import com.suntide_20210418.advancedmemorycard.datagen.ModZhcnLangProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = AdvancedMemoryCardMod.MOD_ID)
public class ModDataGenerator {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        generator.addProvider(event.includeServer(), new ModRecipesProvider(packOutput, event.getLookupProvider()));

        generator.addProvider(
                event.includeClient(), new ModItemModelsProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new ModEnusLangProvider(packOutput));
        generator.addProvider(event.includeClient(), new ModZhcnLangProvider(packOutput));
    }
}
