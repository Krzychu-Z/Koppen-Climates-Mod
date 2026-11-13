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

    public static final TreeGrower FIG = new TreeGrower(KoppenClimateMod.MOD_ID + ":fig",
            Optional.empty(), Optional.of(key("fig")), Optional.empty());
    public static final TreeGrower LEMON = new TreeGrower(KoppenClimateMod.MOD_ID + ":lemon",
            Optional.empty(), Optional.of(key("lemon")), Optional.empty());

    // the Joshua tree of the Lush desert (BSh)
    public static final TreeGrower JOSHUA = new TreeGrower(KoppenClimateMod.MOD_ID + ":joshua",
            Optional.empty(), Optional.of(key("joshua_tree")), Optional.empty());

    // the rainbow eucalyptus of the Australian desert (BSh), with its koalas (worldgen/feature/EucalyptusTreeFeature)
    public static final TreeGrower EUCALYPTUS = new TreeGrower(KoppenClimateMod.MOD_ID + ":eucalyptus",
            Optional.empty(), Optional.of(key("eucalyptus_tree")), Optional.empty());

    // the Subtropical plains' (Cfa) trees (MediterraneanTreeFeature)
    public static final TreeGrower APPLE = new TreeGrower(KoppenClimateMod.MOD_ID + ":apple",
            Optional.empty(), Optional.of(key("apple")), Optional.empty());
    public static final TreeGrower APRICOT = new TreeGrower(KoppenClimateMod.MOD_ID + ":apricot",
            Optional.empty(), Optional.of(key("apricot")), Optional.empty());
    public static final TreeGrower BLACK_POPLAR = new TreeGrower(KoppenClimateMod.MOD_ID + ":black_poplar",
            Optional.empty(), Optional.of(key("black_poplar")), Optional.empty());
    public static final TreeGrower PLATANUS = new TreeGrower(KoppenClimateMod.MOD_ID + ":platanus",
            Optional.empty(), Optional.of(key("platanus")), Optional.empty());
    // the Monsoon subtropical's (Cwa) southern magnolia
    public static final TreeGrower MAGNOLIA = new TreeGrower(KoppenClimateMod.MOD_ID + ":magnolia",
            Optional.empty(), Optional.of(key("magnolia")), Optional.empty());
    // the Temperate forest's (Cfb) maple and Scots pine
    public static final TreeGrower MAPLE = new TreeGrower(KoppenClimateMod.MOD_ID + ":maple",
            Optional.empty(), Optional.of(key("maple")), Optional.empty());
    public static final TreeGrower SCOTS_PINE = new TreeGrower(KoppenClimateMod.MOD_ID + ":scots_pine",
            Optional.empty(), Optional.of(key("scots_pine")), Optional.empty());
    public static final TreeGrower CEDAR = new TreeGrower(KoppenClimateMod.MOD_ID + ":cedar",
            Optional.empty(), Optional.of(key("cedar")), Optional.empty());
    // the redwood: never grown through this (RedwoodSaplingBlock grows it from a 3x3 square), but a sapling needs one
    public static final TreeGrower REDWOOD = new TreeGrower(KoppenClimateMod.MOD_ID + ":redwood",
            Optional.empty(), Optional.of(key("redwood_tree")), Optional.empty());

    private static ResourceKey<ConfiguredFeature<?, ?>> key(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, name));
    }
}
