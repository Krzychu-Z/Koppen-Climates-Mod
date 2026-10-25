package net.krisplis.koppenclimate.item;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.entity.ModEntities;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, KoppenClimateMod.MOD_ID);

    // Eaten like an apple; right-clicked onto the side of a banana stem it starts a new bunch,
    // the way sweet berries work
    public static final RegistryObject<Item> BANANA = ITEMS.register("banana",
            () -> new ItemNameBlockItem(ModBlocks.BANANA_BUNCH.get(), new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.3F).build())));

    // Eaten for a little food; right-clicked onto the side of a coconut palm log it starts new coconuts
    public static final RegistryObject<Item> COCONUT = ITEMS.register("coconut",
            () -> new ItemNameBlockItem(ModBlocks.COCONUT.get(), new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.6F).build())));

    // dropped now and then by a rafflesia; brews into the Potion of Delirium (ModPotions)
    public static final RegistryObject<Item> RAFFLESIA_PETAL = ITEMS.register("rafflesia_petal",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> BLACK_PANTHER_SPAWN_EGG = ITEMS.register("black_panther_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.BLACK_PANTHER, 0x16161A, 0x3A3A42, new Item.Properties()));

    // olive body with dark blotches
    public static final RegistryObject<Item> ANACONDA_SPAWN_EGG = ITEMS.register("anaconda_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ANACONDA, 0x5A6B2E, 0x1F2414, new Item.Properties()));

    // dark olive with pale scales
    public static final RegistryObject<Item> CROCODILE_SPAWN_EGG = ITEMS.register("crocodile_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CROCODILE, 0x3C4628, 0xC4BC8C, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
