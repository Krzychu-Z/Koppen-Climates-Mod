package net.krisplis.koppenclimate.item;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.entity.ModEntities;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.SignItem;
import net.krisplis.koppenclimate.entity.boat.ModBoatItem;
import net.krisplis.koppenclimate.entity.boat.ModBoatWood;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, KoppenClimateMod.MOD_ID);

    // Fills you up like a carrot (3 hunger, 0.6 saturation); right-clicked onto the side of a banana stem
    // it starts a new bunch instead, the way sweet berries work, and onto the top of soil it plants a banana plant
    public static final RegistryObject<Item> BANANA = ITEMS.register("banana",
            () -> new net.krisplis.koppenclimate.item.custom.BananaItem(ModBlocks.BANANA_BUNCH.get(), new Item.Properties()
                    .food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.6F).build())));

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

    // silver with a red belly
    public static final RegistryObject<Item> PIRANHA_SPAWN_EGG = ITEMS.register("piranha_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.PIRANHA, 0x9EA7A6, 0xC23B26, new Item.Properties()));

    public static final RegistryObject<Item> PIRANHA_BUCKET = ITEMS.register("piranha_bucket",
            () -> new MobBucketItem(ModEntities.PIRANHA, () -> Fluids.WATER, () -> SoundEvents.BUCKET_EMPTY_FISH,
                    new Item.Properties().stacksTo(1).component(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY)));

    // places the two-block sleeping mat, like a bed
    public static final RegistryObject<Item> SLEEPING_MAT = ITEMS.register("sleeping_mat",
            () -> new BedItem(ModBlocks.SLEEPING_MAT.get(), new Item.Properties().stacksTo(1)));

    // ------------------------------------------------------------------ rice
    // planted in a block of water over dirt, like seeds in farmland
    public static final RegistryObject<Item> RICE_SEEDS = ITEMS.register("rice_seeds",
            () -> new ItemNameBlockItem(ModBlocks.RICE.get(), new Item.Properties()));
    public static final RegistryObject<Item> RICE = ITEMS.register("rice",
            () -> new Item(new Item.Properties()));
    // three rice and a bowl: fills a third of the hunger bar (7 of its 20 points) and gives the bowl back
    public static final RegistryObject<Item> BOWL_OF_RICE = ITEMS.register("bowl_of_rice",
            () -> new Item(new Item.Properties().stacksTo(1)
                    .food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.6F)
                            .usingConvertsTo(net.minecraft.world.item.Items.BOWL).build())));

    // ------------------------------------------------------------------ jade
    public static final RegistryObject<Item> JADE = ITEMS.register("jade",
            () -> new Item(new Item.Properties()));
    // jade set in gold: worn on the right hand, no poison takes hold and it gives two golden hearts
    public static final RegistryObject<Item> JADE_RING = ITEMS.register("jade_ring",
            () -> new net.krisplis.koppenclimate.item.custom.JadeRingItem(new Item.Properties().stacksTo(1)
                    .rarity(net.minecraft.world.item.Rarity.RARE)));

    // dark brown with a sandy belly and hood
    public static final RegistryObject<Item> COBRA_SPAWN_EGG = ITEMS.register("cobra_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.COBRA, 0x3A2E1E, 0xD9C28A, new Item.Properties()));

    // ------------------------------------------------------------------ mahogany and coconut palm wood
    public static final RegistryObject<Item> MAHOGANY_DOOR = ITEMS.register("mahogany_door",
            () -> new DoubleHighBlockItem(ModBlocks.MAHOGANY_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> MAHOGANY_SIGN = ITEMS.register("mahogany_sign",
            () -> new SignItem(new Item.Properties().stacksTo(16), ModBlocks.MAHOGANY_SIGN.get(), ModBlocks.MAHOGANY_WALL_SIGN.get()));
    public static final RegistryObject<Item> MAHOGANY_HANGING_SIGN = ITEMS.register("mahogany_hanging_sign",
            () -> new HangingSignItem(ModBlocks.MAHOGANY_HANGING_SIGN.get(), ModBlocks.MAHOGANY_WALL_HANGING_SIGN.get(), new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> MAHOGANY_BOAT = ITEMS.register("mahogany_boat",
            () -> new ModBoatItem(false, ModBoatWood.MAHOGANY, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MAHOGANY_CHEST_BOAT = ITEMS.register("mahogany_chest_boat",
            () -> new ModBoatItem(true, ModBoatWood.MAHOGANY, new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BAOBAB_DOOR = ITEMS.register("baobab_door",
            () -> new DoubleHighBlockItem(ModBlocks.BAOBAB_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> BAOBAB_SIGN = ITEMS.register("baobab_sign",
            () -> new SignItem(new Item.Properties().stacksTo(16), ModBlocks.BAOBAB_SIGN.get(), ModBlocks.BAOBAB_WALL_SIGN.get()));
    public static final RegistryObject<Item> BAOBAB_HANGING_SIGN = ITEMS.register("baobab_hanging_sign",
            () -> new HangingSignItem(ModBlocks.BAOBAB_HANGING_SIGN.get(), ModBlocks.BAOBAB_WALL_HANGING_SIGN.get(), new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> BAOBAB_BOAT = ITEMS.register("baobab_boat",
            () -> new ModBoatItem(false, ModBoatWood.BAOBAB, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> BAOBAB_CHEST_BOAT = ITEMS.register("baobab_chest_boat",
            () -> new ModBoatItem(true, ModBoatWood.BAOBAB, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> OLIVE_DOOR = ITEMS.register("olive_door",
            () -> new DoubleHighBlockItem(ModBlocks.OLIVE_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> OLIVE_SIGN = ITEMS.register("olive_sign",
            () -> new SignItem(new Item.Properties().stacksTo(16), ModBlocks.OLIVE_SIGN.get(), ModBlocks.OLIVE_WALL_SIGN.get()));
    public static final RegistryObject<Item> OLIVE_HANGING_SIGN = ITEMS.register("olive_hanging_sign",
            () -> new HangingSignItem(ModBlocks.OLIVE_HANGING_SIGN.get(), ModBlocks.OLIVE_WALL_HANGING_SIGN.get(), new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> OLIVE_BOAT = ITEMS.register("olive_boat",
            () -> new ModBoatItem(false, ModBoatWood.OLIVE, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> OLIVE_CHEST_BOAT = ITEMS.register("olive_chest_boat",
            () -> new ModBoatItem(true, ModBoatWood.OLIVE, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ITALIAN_PINE_DOOR = ITEMS.register("italian_pine_door",
            () -> new DoubleHighBlockItem(ModBlocks.ITALIAN_PINE_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> ITALIAN_PINE_SIGN = ITEMS.register("italian_pine_sign",
            () -> new SignItem(new Item.Properties().stacksTo(16), ModBlocks.ITALIAN_PINE_SIGN.get(), ModBlocks.ITALIAN_PINE_WALL_SIGN.get()));
    public static final RegistryObject<Item> ITALIAN_PINE_HANGING_SIGN = ITEMS.register("italian_pine_hanging_sign",
            () -> new HangingSignItem(ModBlocks.ITALIAN_PINE_HANGING_SIGN.get(), ModBlocks.ITALIAN_PINE_WALL_HANGING_SIGN.get(), new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> ITALIAN_PINE_BOAT = ITEMS.register("italian_pine_boat",
            () -> new ModBoatItem(false, ModBoatWood.ITALIAN_PINE, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ITALIAN_PINE_CHEST_BOAT = ITEMS.register("italian_pine_chest_boat",
            () -> new ModBoatItem(true, ModBoatWood.ITALIAN_PINE, new Item.Properties().stacksTo(1)));
    // ------------------------------------------------------------------ the Mediterranean's harvest
    // an olive, picked from olive leaves: a mouthful of food, but mostly for pressing into oil
    public static final RegistryObject<Item> OLIVE = ITEMS.register("olive",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.3F).build())));
    // olives and a glass bottle make olive oil: drinkable for half a heart, and a cooking ingredient
    public static final RegistryObject<Item> OLIVE_OIL = ITEMS.register("olive_oil",
            () -> new net.krisplis.koppenclimate.item.custom.OliveOilItem(new Item.Properties().stacksTo(16)
                    .craftRemainder(net.minecraft.world.item.Items.GLASS_BOTTLE)));
    // a sprig of wild rosemary, and the oil pressed from it
    public static final RegistryObject<Item> ROSEMARY_SPRIG = ITEMS.register("rosemary_sprig", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ROSEMARY_OIL = ITEMS.register("rosemary_oil",
            () -> new Item(new Item.Properties().stacksTo(16).craftRemainder(net.minecraft.world.item.Items.GLASS_BOTTLE)));
    public static final RegistryObject<Item> SCORPION_SPAWN_EGG = ITEMS.register("scorpion_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(net.krisplis.koppenclimate.entity.ModEntities.SCORPION, 0x8A6A3A, 0x3A2A1A, new Item.Properties()));
    // ------------------------------------------------------------------ the African savanna's animals
    // an elephant's tusk
    // a lion's fur
    public static final RegistryObject<Item> FUR = ITEMS.register("fur", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> IVORY_TUSK = ITEMS.register("ivory_tusk", () -> new Item(new Item.Properties()));
    // a plate of rhino hide, for rhino plate armour
    public static final RegistryObject<Item> RHINO_PLATE = ITEMS.register("rhino_plate", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RHINO_PLATE_HELMET = ITEMS.register("rhino_plate_helmet",
            () -> new net.minecraft.world.item.ArmorItem(ModArmorMaterials.RHINO_PLATE, net.minecraft.world.item.ArmorItem.Type.HELMET,
                    new Item.Properties().durability(net.minecraft.world.item.ArmorItem.Type.HELMET.getDurability(28))));
    public static final RegistryObject<Item> RHINO_PLATE_CHESTPLATE = ITEMS.register("rhino_plate_chestplate",
            () -> new net.minecraft.world.item.ArmorItem(ModArmorMaterials.RHINO_PLATE, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE.getDurability(28))));
    public static final RegistryObject<Item> RHINO_PLATE_LEGGINGS = ITEMS.register("rhino_plate_leggings",
            () -> new net.minecraft.world.item.ArmorItem(ModArmorMaterials.RHINO_PLATE, net.minecraft.world.item.ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(net.minecraft.world.item.ArmorItem.Type.LEGGINGS.getDurability(28))));
    public static final RegistryObject<Item> RHINO_PLATE_BOOTS = ITEMS.register("rhino_plate_boots",
            () -> new net.minecraft.world.item.ArmorItem(ModArmorMaterials.RHINO_PLATE, net.minecraft.world.item.ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(net.minecraft.world.item.ArmorItem.Type.BOOTS.getDurability(28))));
    public static final RegistryObject<Item> BLACK_MAMBA_SPAWN_EGG = ITEMS.register("black_mamba_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(net.krisplis.koppenclimate.entity.ModEntities.BLACK_MAMBA, 0x55554A, 0x2A2A26, new Item.Properties()));
    public static final RegistryObject<Item> GIRAFFE_SPAWN_EGG = ITEMS.register("giraffe_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(net.krisplis.koppenclimate.entity.ModEntities.GIRAFFE, 0xD6AC64, 0x804C24, new Item.Properties()));
    public static final RegistryObject<Item> ELEPHANT_SPAWN_EGG = ITEMS.register("elephant_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(net.krisplis.koppenclimate.entity.ModEntities.ELEPHANT, 0x7E7A76, 0xECE2C4, new Item.Properties()));
    public static final RegistryObject<Item> RHINO_SPAWN_EGG = ITEMS.register("rhino_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(net.krisplis.koppenclimate.entity.ModEntities.RHINO, 0x7C746A, 0xAAA08C, new Item.Properties()));
    public static final RegistryObject<Item> LION_SPAWN_EGG = ITEMS.register("lion_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(net.krisplis.koppenclimate.entity.ModEntities.LION, 0xC49652, 0x704622, new Item.Properties()));
    public static final RegistryObject<Item> ZEBRA_SPAWN_EGG = ITEMS.register("zebra_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(net.krisplis.koppenclimate.entity.ModEntities.ZEBRA, 0xEEEEEE, 0x202020, new Item.Properties()));
    public static final RegistryObject<Item> JACKAL_SPAWN_EGG = ITEMS.register("jackal_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(net.krisplis.koppenclimate.entity.ModEntities.JACKAL, 0xC0904E, 0x3A3430, new Item.Properties()));
    public static final RegistryObject<Item> TERMITE_SPAWN_EGG = ITEMS.register("termite_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(net.krisplis.koppenclimate.entity.ModEntities.TERMITE, 0xC9A27A, 0x7A3E1E, new Item.Properties()));
    public static final RegistryObject<Item> COCONUT_PALM_DOOR = ITEMS.register("coconut_palm_door",
            () -> new DoubleHighBlockItem(ModBlocks.COCONUT_PALM_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> COCONUT_PALM_SIGN = ITEMS.register("coconut_palm_sign",
            () -> new SignItem(new Item.Properties().stacksTo(16), ModBlocks.COCONUT_PALM_SIGN.get(), ModBlocks.COCONUT_PALM_WALL_SIGN.get()));
    public static final RegistryObject<Item> COCONUT_PALM_HANGING_SIGN = ITEMS.register("coconut_palm_hanging_sign",
            () -> new HangingSignItem(ModBlocks.COCONUT_PALM_HANGING_SIGN.get(), ModBlocks.COCONUT_PALM_WALL_HANGING_SIGN.get(), new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> COCONUT_PALM_BOAT = ITEMS.register("coconut_palm_boat",
            () -> new ModBoatItem(false, ModBoatWood.COCONUT_PALM, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> COCONUT_PALM_CHEST_BOAT = ITEMS.register("coconut_palm_chest_boat",
            () -> new ModBoatItem(true, ModBoatWood.COCONUT_PALM, new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
