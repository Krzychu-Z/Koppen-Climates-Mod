package net.krisplis.koppenclimate.worldgen.tree;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModTreePlacers {
    public static final DeferredRegister<TrunkPlacerType<?>> TRUNK_PLACERS =
            DeferredRegister.create(Registries.TRUNK_PLACER_TYPE, KoppenClimateMod.MOD_ID);
    public static final DeferredRegister<FoliagePlacerType<?>> FOLIAGE_PLACERS =
            DeferredRegister.create(Registries.FOLIAGE_PLACER_TYPE, KoppenClimateMod.MOD_ID);
    public static final DeferredRegister<TreeDecoratorType<?>> TREE_DECORATORS =
            DeferredRegister.create(Registries.TREE_DECORATOR_TYPE, KoppenClimateMod.MOD_ID);

    public static final RegistryObject<TrunkPlacerType<PalmTrunkPlacer>> PALM_TRUNK_PLACER =
            TRUNK_PLACERS.register("palm_trunk_placer", () -> new TrunkPlacerType<>(PalmTrunkPlacer.CODEC));
    public static final RegistryObject<FoliagePlacerType<PalmFoliagePlacer>> PALM_FOLIAGE_PLACER =
            FOLIAGE_PLACERS.register("palm_foliage_placer", () -> new FoliagePlacerType<>(PalmFoliagePlacer.CODEC));

    public static final RegistryObject<TrunkPlacerType<MahoganyTrunkPlacer>> MAHOGANY_TRUNK_PLACER =
            TRUNK_PLACERS.register("mahogany_trunk_placer", () -> new TrunkPlacerType<>(MahoganyTrunkPlacer.CODEC));
    public static final RegistryObject<FoliagePlacerType<MahoganyFoliagePlacer>> MAHOGANY_FOLIAGE_PLACER =
            FOLIAGE_PLACERS.register("mahogany_foliage_placer", () -> new FoliagePlacerType<>(MahoganyFoliagePlacer.CODEC));

    public static final RegistryObject<FoliagePlacerType<BananaFoliagePlacer>> BANANA_FOLIAGE_PLACER =
            FOLIAGE_PLACERS.register("banana_foliage_placer", () -> new FoliagePlacerType<>(BananaFoliagePlacer.CODEC));
    // banana bunches and coconuts
    public static final RegistryObject<TreeDecoratorType<HangingFruitDecorator>> HANGING_FRUIT_DECORATOR =
            TREE_DECORATORS.register("hanging_fruit", () -> new TreeDecoratorType<>(HangingFruitDecorator.CODEC));
    public static final RegistryObject<TreeDecoratorType<LianaDecorator>> LIANA_DECORATOR =
            TREE_DECORATORS.register("liana", () -> new TreeDecoratorType<>(LianaDecorator.CODEC));

    public static void register(IEventBus eventBus) {
        TRUNK_PLACERS.register(eventBus);
        FOLIAGE_PLACERS.register(eventBus);
        TREE_DECORATORS.register(eventBus);
    }
}
