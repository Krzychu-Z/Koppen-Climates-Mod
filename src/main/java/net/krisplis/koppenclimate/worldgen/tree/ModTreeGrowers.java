package net.krisplis.koppenclimate.worldgen.tree;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.Optional;

public class ModTreeGrowers {
    // Picks one of the three palm shapes at random, see worldgen/configured_feature/coconut_palm.json
    public static final ResourceKey<ConfiguredFeature<?, ?>> COCONUT_PALM_KEY = ResourceKey.create(Registries.CONFIGURED_FEATURE,
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "coconut_palm"));

    public static final TreeGrower COCONUT_PALM = new TreeGrower(KoppenClimateMod.MOD_ID + ":coconut_palm",
            Optional.empty(), Optional.of(COCONUT_PALM_KEY), Optional.empty());

    // One sapling grows the tall mahogany; four saplings in a 2x2 square grow the giant, like jungle trees
    public static final ResourceKey<ConfiguredFeature<?, ?>> MAHOGANY_KEY = key("mahogany_tall");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MAHOGANY_GIANT_KEY = key("mahogany_giant");

    public static final TreeGrower MAHOGANY = new TreeGrower(KoppenClimateMod.MOD_ID + ":mahogany",
            Optional.of(MAHOGANY_GIANT_KEY), Optional.of(MAHOGANY_KEY), Optional.empty());

    public static final ResourceKey<ConfiguredFeature<?, ?>> BANANA_KEY = key("banana_plant");

    public static final TreeGrower BANANA = new TreeGrower(KoppenClimateMod.MOD_ID + ":banana",
            Optional.empty(), Optional.of(BANANA_KEY), Optional.empty());

    // the baobab, from a single sapling
    public static final ResourceKey<ConfiguredFeature<?, ?>> BAOBAB_KEY = key("baobab");

    public static final TreeGrower BAOBAB = new TreeGrower(KoppenClimateMod.MOD_ID + ":baobab",
            Optional.empty(), Optional.of(BAOBAB_KEY), Optional.empty());

    // the Mediterranean trees. The Italian pine grows one of two shapes: mostly the tall umbrella, sometimes the
    // broad forked one
    public static final ResourceKey<ConfiguredFeature<?, ?>> OLIVE_KEY = key("olive");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ITALIAN_PINE_KEY = key("italian_pine_umbrella");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ITALIAN_PINE_WIDE_KEY = key("italian_pine_wide");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CYPRESS_KEY = key("cypress");

    public static final TreeGrower OLIVE = new TreeGrower(KoppenClimateMod.MOD_ID + ":olive",
            Optional.empty(), Optional.of(OLIVE_KEY), Optional.empty());
    public static final TreeGrower ITALIAN_PINE = new TreeGrower(KoppenClimateMod.MOD_ID + ":italian_pine", 0.35F,
            Optional.empty(), Optional.empty(), Optional.of(ITALIAN_PINE_KEY), Optional.of(ITALIAN_PINE_WIDE_KEY), Optional.empty(), Optional.empty());
    public static final TreeGrower CYPRESS = new TreeGrower(KoppenClimateMod.MOD_ID + ":cypress",
            Optional.empty(), Optional.of(CYPRESS_KEY), Optional.empty());

    private static ResourceKey<ConfiguredFeature<?, ?>> key(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, name));
    }
}
