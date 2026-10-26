package net.krisplis.koppenclimate.village;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Everything the Amazon tribal villages add: the villager, its spawn egg, and the village structure. */
public final class VillageRegistry {
    private VillageRegistry() {
    }

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, KoppenClimateMod.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, KoppenClimateMod.MOD_ID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, KoppenClimateMod.MOD_ID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, KoppenClimateMod.MOD_ID);

    // a villager's size
    public static final RegistryObject<EntityType<AmazonVillagerEntity>> AMAZON_VILLAGER = ENTITY_TYPES.register("amazon_villager",
            () -> EntityType.Builder.of(AmazonVillagerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.62F)
                    .clientTrackingRange(10)
                    .build("amazon_villager"));

    // warm brown with red annatto paint
    public static final RegistryObject<Item> AMAZON_VILLAGER_SPAWN_EGG = ITEMS.register("amazon_villager_spawn_egg",
            () -> new ForgeSpawnEggItem(AMAZON_VILLAGER, 0x8A5A3C, 0xB8322A, new Item.Properties()));

    public static final RegistryObject<StructureType<AmazonVillageStructure>> AMAZON_VILLAGE =
            STRUCTURE_TYPES.register("amazon_village", () -> () -> AmazonVillageStructure.CODEC);

    public static final RegistryObject<StructurePieceType> AMAZON_VILLAGE_PIECE =
            STRUCTURE_PIECES.register("amazon_village", () -> (StructurePieceType.ContextlessType) AmazonVillagePiece::new);

    // ---------------------------------------------------------------- Maasai settlements of the African savanna

    public static final RegistryObject<EntityType<AfricanVillagerEntity>> AFRICAN_VILLAGER = ENTITY_TYPES.register("african_villager",
            () -> EntityType.Builder.of(AfricanVillagerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.62F)
                    .clientTrackingRange(10)
                    .build("african_villager"));

    // dark brown with the red of the shuka
    public static final RegistryObject<Item> AFRICAN_VILLAGER_SPAWN_EGG = ITEMS.register("african_villager_spawn_egg",
            () -> new ForgeSpawnEggItem(AFRICAN_VILLAGER, 0x4A2E1E, 0xC0262B, new Item.Properties()));

    public static final RegistryObject<StructureType<AfricanVillageStructure>> AFRICAN_VILLAGE =
            STRUCTURE_TYPES.register("african_village", () -> () -> AfricanVillageStructure.CODEC);

    public static final RegistryObject<StructurePieceType> AFRICAN_VILLAGE_PIECE =
            STRUCTURE_PIECES.register("african_village", () -> (StructurePieceType.ContextlessType) AfricanVillagePiece::new);

    // ---------------------------------------------------------------- Thai floating villages

    public static final DeferredRegister<VillagerType> VILLAGER_TYPES =
            DeferredRegister.create(Registries.VILLAGER_TYPE, KoppenClimateMod.MOD_ID);

    // ordinary villagers, in the plain clothes of a poor Thai village (textures/entity/villager/type/thai.png)
    public static final RegistryObject<VillagerType> THAI = VILLAGER_TYPES.register("thai", () -> new VillagerType("thai"));

    public static final RegistryObject<StructureType<ThaiVillageStructure>> THAI_VILLAGE =
            STRUCTURE_TYPES.register("thai_village", () -> () -> ThaiVillageStructure.CODEC);

    public static final RegistryObject<StructurePieceType> THAI_VILLAGE_PIECE =
            STRUCTURE_PIECES.register("thai_village", () -> (StructurePieceType.ContextlessType) ThaiVillagePiece::new);

    public static void register(IEventBus eventBus) {
        VILLAGER_TYPES.register(eventBus);
        ENTITY_TYPES.register(eventBus);
        ITEMS.register(eventBus);
        STRUCTURE_TYPES.register(eventBus);
        STRUCTURE_PIECES.register(eventBus);
        eventBus.addListener(VillageRegistry::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(AMAZON_VILLAGER.get(), AmazonVillagerEntity.createAttributes().build());
        event.put(AFRICAN_VILLAGER.get(), AmazonVillagerEntity.createAttributes().build());
    }
}
