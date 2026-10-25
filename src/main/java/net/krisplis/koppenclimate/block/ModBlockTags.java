package net.krisplis.koppenclimate.block;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModBlockTags {
    public static final TagKey<Block> COCONUT_PALM_PLANTABLE_ON = create("coconut_palm_plantable_on");
    // blocks a banana bunch can hang on
    public static final TagKey<Block> BANANA_STEMS = create("banana_stems");
    // blocks coconuts can hang on
    public static final TagKey<Block> COCONUT_PALM_LOGS = create("coconut_palm_logs");

    private static TagKey<Block> create(String name) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, name));
    }
}
