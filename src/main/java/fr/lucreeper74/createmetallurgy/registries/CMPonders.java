package fr.lucreeper74.createmetallurgy.registries;

import com.simibubi.create.foundation.ponder.PonderLocalization;
import com.simibubi.create.foundation.ponder.PonderRegistrationHelper;
import com.simibubi.create.infrastructure.ponder.AllPonderTags;
import fr.lucreeper74.createmetallurgy.CreateMetallurgy;
import fr.lucreeper74.createmetallurgy.ponders.CastingScenes;
import fr.lucreeper74.createmetallurgy.ponders.FoundryScenes;
import fr.lucreeper74.createmetallurgy.ponders.LightBulbScenes;

public class CMPonders {
    static final PonderRegistrationHelper HELPER = new PonderRegistrationHelper(CreateMetallurgy.MOD_ID);

    public static void register() {
        // Register storyboards here
        // (!) Added entries require re-launch
        // (!) Modifications inside storyboard methods only require re-opening the ui

        HELPER.forComponents(CMBlocks.FOUNDRY_BASIN_BLOCK)
                .addStoryBoard("foundry_basin", FoundryScenes::foundryBasin, CMPonderTags.METALWORK)
                .addStoryBoard("foundry_mixer", FoundryScenes::alloying, CMPonderTags.METALWORK);

        HELPER.forComponents(CMBlocks.FOUNDRY_MIXER_BLOCK)
                .addStoryBoard("foundry_mixer", FoundryScenes::alloying, CMPonderTags.METALWORK);


        HELPER.forComponents(CMBlocks.CASTING_BASIN_BLOCK, CMBlocks.CASTING_TABLE_BLOCK)
                .addStoryBoard("casting_blocks", CastingScenes::castingBlocks, CMPonderTags.METALWORK);

        HELPER.forComponents(CMBlocks.LIGHT_BULBS.toArray())
                .addStoryBoard("light_bulbs", LightBulbScenes::lightBulbScenes, AllPonderTags.REDSTONE);
    }

    public static void registerLang() {
        PonderLocalization.provideRegistrateLang(CreateMetallurgy.REGISTRATE);
    }
}
