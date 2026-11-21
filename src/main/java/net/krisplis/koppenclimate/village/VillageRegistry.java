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

    // ---------------------------------------------------------------- Lakota camps of the Prairie (Dfa)

    public static final RegistryObject<EntityType<LakotaVillagerEntity>> LAKOTA_VILLAGER = ENTITY_TYPES.register("lakota_villager",
            () -> EntityType.Builder.of(LakotaVillagerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.62F)
                    .clientTrackingRange(10)
                    .build("lakota_villager"));

    // buckskin tan with the blue of the beadwork
    public static final RegistryObject<Item> LAKOTA_VILLAGER_SPAWN_EGG = ITEMS.register("lakota_villager_spawn_egg",
            () -> new ForgeSpawnEggItem(LAKOTA_VILLAGER, 0xC8A878, 0x2A6AB0, new Item.Properties()));

    public static final RegistryObject<StructureType<LakotaCampStructure>> LAKOTA_CAMP =
            STRUCTURE_TYPES.register("lakota_camp", () -> () -> LakotaCampStructure.CODEC);

    public static final RegistryObject<StructurePieceType> LAKOTA_CAMP_PIECE =
            STRUCTURE_PIECES.register("lakota_camp", () -> (StructurePieceType.ContextlessType) LakotaCampPiece::new);

    // ---------------------------------------------------------------- Thai floating villages

    public static final DeferredRegister<VillagerType> VILLAGER_TYPES =
            DeferredRegister.create(Registries.VILLAGER_TYPE, KoppenClimateMod.MOD_ID);

    // ordinary villagers, in the plain clothes of a poor Thai village (textures/entity/villager/type/thai.png)
    public static final RegistryObject<VillagerType> THAI = VILLAGER_TYPES.register("thai", () -> new VillagerType("thai"));

    // ---------------------------------------------------------------- Tuscan hill towns of the Mediterranean (Csa)

    // villagers dressed as 16th-century Venetians (textures/entity/villager/type/venetian.png)
    public static final RegistryObject<VillagerType> VENETIAN = VILLAGER_TYPES.register("venetian", () -> new VillagerType("venetian"));

    public static final RegistryObject<StructureType<TuscanVillageStructure>> TUSCAN_VILLAGE =
            STRUCTURE_TYPES.register("tuscan_village", () -> () -> TuscanVillageStructure.CODEC);

    public static final RegistryObject<StructurePieceType> TUSCAN_VILLAGE_PIECE =
            STRUCTURE_PIECES.register("tuscan_village", () -> (StructurePieceType.ContextlessType) TuscanVillagePiece::new);

    // ---------------------------------------------------------------- Dalmatian island towns off the Mediterranean beaches

    public static final RegistryObject<StructureType<CroatianVillageStructure>> CROATIAN_VILLAGE =
            STRUCTURE_TYPES.register("croatian_village", () -> () -> CroatianVillageStructure.CODEC);

    public static final RegistryObject<StructurePieceType> CROATIAN_VILLAGE_PIECE =
            STRUCTURE_PIECES.register("croatian_village", () -> (StructurePieceType.ContextlessType) CroatianVillagePiece::new);

    // the ghost towns of the Lush desert (BSh)
    public static final RegistryObject<StructureType<GhostTownStructure>> GHOST_TOWN =
            STRUCTURE_TYPES.register("ghost_town", () -> () -> GhostTownStructure.CODEC);
    public static final RegistryObject<StructurePieceType> GHOST_TOWN_PIECE =
            STRUCTURE_PIECES.register("ghost_town", () -> (StructurePieceType.ContextlessType) GhostTownPiece::new);

    // the red farms of the American farmland (Cfa)
    public static final RegistryObject<StructureType<RedFarmStructure>> RED_FARM =
            STRUCTURE_TYPES.register("red_farm", () -> () -> RedFarmStructure.CODEC);
    public static final RegistryObject<StructurePieceType> RED_FARM_PIECE =
            STRUCTURE_PIECES.register("red_farm", () -> (StructurePieceType.ContextlessType) RedFarmPiece::new);

    // ---------------------------------------------------------------- Chinese villages of the monsoon subtropics (Cwa)

    // villagers in the hemp and silk of old China (textures/entity/villager/type/chinese.png, chinese_profession/)
    public static final RegistryObject<VillagerType> CHINESE = VILLAGER_TYPES.register("chinese", () -> new VillagerType("chinese"));

    public static final RegistryObject<StructureType<ChineseVillageStructure>> CHINESE_VILLAGE =
            STRUCTURE_TYPES.register("chinese_village", () -> () -> ChineseVillageStructure.CODEC);

    public static final RegistryObject<StructurePieceType> CHINESE_VILLAGE_PIECE =
            STRUCTURE_PIECES.register("chinese_village", () -> (StructurePieceType.ContextlessType) ChineseVillagePiece::new);

    // ---------------------------------------------------------------- Japanese villages of the Cherry orchards (Cwa)

    // villagers in kimono and hakama (textures/entity/villager/type/japanese.png, japanese_profession/)
    public static final RegistryObject<VillagerType> JAPANESE = VILLAGER_TYPES.register("japanese", () -> new VillagerType("japanese"));

    public static final RegistryObject<StructureType<JapaneseVillageStructure>> JAPANESE_VILLAGE =
            STRUCTURE_TYPES.register("japanese_village", () -> () -> JapaneseVillageStructure.CODEC);

    public static final RegistryObject<StructurePieceType> JAPANESE_VILLAGE_PIECE =
            STRUCTURE_PIECES.register("japanese_village", () -> (StructurePieceType.ContextlessType) JapaneseVillagePiece::new);

    // ---------------------------------------------------------------- German towns of the Continental forest (Dfb)

    // villagers in the Jäger's loden and the working dress of western Germany (textures/entity/villager/type/german.png,
    // german_profession/)
    public static final RegistryObject<VillagerType> GERMAN = VILLAGER_TYPES.register("german", () -> new VillagerType("german"));

    public static final RegistryObject<StructureType<GermanVillageStructure>> GERMAN_VILLAGE =
            STRUCTURE_TYPES.register("german_village", () -> () -> GermanVillageStructure.CODEC);

    public static final RegistryObject<StructurePieceType> GERMAN_VILLAGE_PIECE =
            STRUCTURE_PIECES.register("german_village", () -> (StructurePieceType.ContextlessType) GermanVillagePiece::new);

    // ---------------------------------------------------------------- Tirol villages of the Alpine valleys (Dfb)

    // villagers in Tyrolean Tracht (textures/entity/villager/type/tirol.png, tirol_profession/)
    public static final RegistryObject<VillagerType> TIROL = VILLAGER_TYPES.register("tirol", () -> new VillagerType("tirol"));

    public static final RegistryObject<StructureType<TirolVillageStructure>> TIROL_VILLAGE =
            STRUCTURE_TYPES.register("tirol_village", () -> () -> TirolVillageStructure.CODEC);

    public static final RegistryObject<StructurePieceType> TIROL_VILLAGE_PIECE =
            STRUCTURE_PIECES.register("tirol_village", () -> (StructurePieceType.ContextlessType) TirolVillagePiece::new);

    // ---------------------------------------------------------------- the Cursed Oasis of the hot deserts

    public static final RegistryObject<StructureType<CursedOasisStructure>> CURSED_OASIS =
            STRUCTURE_TYPES.register("cursed_oasis", () -> () -> CursedOasisStructure.CODEC);

    public static final RegistryObject<StructurePieceType> CURSED_OASIS_PIECE =
            STRUCTURE_PIECES.register("cursed_oasis", () -> (StructurePieceType.ContextlessType) CursedOasisPiece::new);

    // ---------------------------------------------------------------- the abandoned Soviet estate of the subpolar taiga and tundra

    public static final RegistryObject<StructureType<ChernobylStructure>> CHERNOBYL =
            STRUCTURE_TYPES.register("chernobyl", () -> () -> ChernobylStructure.CODEC);

    public static final RegistryObject<StructurePieceType> CHERNOBYL_PIECE =
            STRUCTURE_PIECES.register("chernobyl", () -> (StructurePieceType.ContextlessType) ChernobylPiece::new);

    // ---------------------------------------------------------------- the Highland Castle of the Scottish highlands (Cfc)

    public static final RegistryObject<StructureType<CastleStructure>> HIGHLAND_CASTLE =
            STRUCTURE_TYPES.register("highland_castle", () -> () -> CastleStructure.CODEC);

    public static final RegistryObject<StructurePieceType> CASTLE_PIECE =
            STRUCTURE_PIECES.register("highland_castle", () -> (StructurePieceType.ContextlessType) CastlePiece::new);

    // ---------------------------------------------------------------- the English Mansion of the English countryside (Cfb)

    public static final RegistryObject<StructureType<MansionStructure>> ENGLISH_MANSION =
            STRUCTURE_TYPES.register("english_mansion", () -> () -> MansionStructure.CODEC);

    public static final RegistryObject<StructurePieceType> MANSION_PIECE =
            STRUCTURE_PIECES.register("english_mansion", () -> (StructurePieceType.ContextlessType) MansionPiece::new);

    // ---------------------------------------------------------------- the French village of the temperate forest (Cfb)

    public static final RegistryObject<StructureType<FrenchVillageStructure>> FRENCH_VILLAGE =
            STRUCTURE_TYPES.register("french_village", () -> () -> FrenchVillageStructure.CODEC);

    public static final RegistryObject<StructurePieceType> FRENCH_VILLAGE_PIECE =
            STRUCTURE_PIECES.register("french_village", () -> (StructurePieceType.ContextlessType) FrenchVillagePiece::new);

    // ---------------------------------------------------------------- the Gothic Cathedral of the temperate forest (Cfb)

    public static final RegistryObject<StructureType<CathedralStructure>> GOTHIC_CATHEDRAL =
            STRUCTURE_TYPES.register("gothic_cathedral", () -> () -> CathedralStructure.CODEC);

    public static final RegistryObject<StructurePieceType> CATHEDRAL_PIECE =
            STRUCTURE_PIECES.register("gothic_cathedral", () -> (StructurePieceType.ContextlessType) CathedralPiece::new);

    // ---------------------------------------------------------------- Pompeii, the ever-burning Roman town of the Mediterranean (Csa)

    public static final RegistryObject<StructureType<PompeiiStructure>> POMPEII =
            STRUCTURE_TYPES.register("pompeii", () -> () -> PompeiiStructure.CODEC);

    public static final RegistryObject<StructurePieceType> POMPEII_PIECE =
            STRUCTURE_PIECES.register("pompeii", () -> (StructurePieceType.ContextlessType) PompeiiPiece::new);

    // ---------------------------------------------------------------- Viking villages of the Subpolar taiga (Dfc)

    // villagers in Norse dress (textures/entity/villager/type/viking.png, viking_profession/)
    public static final RegistryObject<VillagerType> VIKING = VILLAGER_TYPES.register("viking", () -> new VillagerType("viking"));

    public static final RegistryObject<StructureType<VikingVillageStructure>> VIKING_VILLAGE =
            STRUCTURE_TYPES.register("viking_village", () -> () -> VikingVillageStructure.CODEC);

    public static final RegistryObject<StructurePieceType> VIKING_VILLAGE_PIECE =
            STRUCTURE_PIECES.register("viking_village", () -> (StructurePieceType.ContextlessType) VikingVillagePiece::new);

    public static final RegistryObject<StructureType<ThaiVillageStructure>> THAI_VILLAGE =
            STRUCTURE_TYPES.register("thai_village", () -> () -> ThaiVillageStructure.CODEC);

    public static final RegistryObject<StructurePieceType> THAI_VILLAGE_PIECE =
            STRUCTURE_PIECES.register("thai_village", () -> (StructurePieceType.ContextlessType) ThaiVillagePiece::new);

    // ---------------------------------------------------------------- desert villages in the mod's desert colours

    public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType<?>> PROCESSORS =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, KoppenClimateMod.MOD_ID);

    public static final RegistryObject<net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType<DesertRecolorProcessor>> DESERT_RECOLOR =
            PROCESSORS.register("desert_recolor", () -> () -> DesertRecolorProcessor.CODEC);

    public static void register(IEventBus eventBus) {
        PROCESSORS.register(eventBus);
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
        event.put(LAKOTA_VILLAGER.get(), AmazonVillagerEntity.createAttributes().build());
    }
}
