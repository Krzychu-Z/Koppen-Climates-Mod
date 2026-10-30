package net.krisplis.koppenclimate.entity;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.boat.ModBoat;
import net.krisplis.koppenclimate.entity.boat.ModChestBoat;
import net.krisplis.koppenclimate.entity.custom.AnacondaEntity;
import net.krisplis.koppenclimate.entity.custom.CobraEntity;
import net.krisplis.koppenclimate.entity.custom.BlackPantherEntity;
import net.krisplis.koppenclimate.entity.custom.CrocodileEntity;
import net.krisplis.koppenclimate.entity.custom.PiranhaEntity;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.krisplis.koppenclimate.event.AmazonSpawns;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, KoppenClimateMod.MOD_ID);

    // The main hitbox is a low box over the middle of the snake; the model is about 5.5 blocks long, so
    // AnacondaEntity lays extra hitboxes (AnacondaPart) along the rest of its body
    public static final RegistryObject<EntityType<AnacondaEntity>> ANACONDA = ENTITY_TYPES.register("anaconda",
            () -> EntityType.Builder.of(AnacondaEntity::new, MobCategory.CREATURE)
                    .sized(1.4F, 0.8F)
                    .clientTrackingRange(10)
                    .build("anaconda"));

    public static final RegistryObject<EntityType<CrocodileEntity>> CROCODILE = ENTITY_TYPES.register("crocodile",
            () -> EntityType.Builder.of(CrocodileEntity::new, MobCategory.CREATURE)
                    .sized(1.4F, 0.7F)
                    .clientTrackingRange(10)
                    .build("crocodile"));

    // an ocelot's hitbox, scaled up with the model
    public static final RegistryObject<EntityType<BlackPantherEntity>> BLACK_PANTHER = ENTITY_TYPES.register("black_panther",
            () -> EntityType.Builder.of(BlackPantherEntity::new, MobCategory.CREATURE)
                    .sized(0.9F, 1.05F)
                    .clientTrackingRange(10)
                    .build("black_panther"));

    // a little bigger than a cod
    public static final RegistryObject<EntityType<PiranhaEntity>> PIRANHA = ENTITY_TYPES.register("piranha",
            () -> EntityType.Builder.of(PiranhaEntity::new, MobCategory.WATER_AMBIENT)
                    .sized(0.5F, 0.45F)
                    .eyeHeight(0.25F)
                    .clientTrackingRange(4)
                    .build("piranha"));

    // an Indian cobra, about two blocks long; the Tropical Monsoon's snake in place of the anaconda
    public static final RegistryObject<EntityType<CobraEntity>> COBRA = ENTITY_TYPES.register("cobra",
            () -> EntityType.Builder.of(CobraEntity::new, MobCategory.CREATURE)
                    .sized(0.8F, 0.4F)
                    .clientTrackingRange(10)
                    .build("cobra"));

    // the Mediterranean (Csa) viper: a cobra's ways in a smaller, milder snake
    public static final RegistryObject<EntityType<net.krisplis.koppenclimate.entity.custom.ViperEntity>> VIPER = ENTITY_TYPES.register("viper",
            () -> EntityType.Builder.of(net.krisplis.koppenclimate.entity.custom.ViperEntity::new, MobCategory.CREATURE)
                    .sized(0.55F, 0.3F)
                    .clientTrackingRange(8)
                    .build("viper"));

    // a shark of the warm seas, some three blocks long
    public static final RegistryObject<EntityType<net.krisplis.koppenclimate.entity.custom.SharkEntity>> SHARK = ENTITY_TYPES.register("shark",
            () -> EntityType.Builder.of(net.krisplis.koppenclimate.entity.custom.SharkEntity::new, MobCategory.WATER_CREATURE)
                    .sized(1.4F, 0.9F)
                    .eyeHeight(0.5F)
                    .clientTrackingRange(10)
                    .build("shark"));

    // a little red crab of the warm beaches, a scorpion's size
    public static final RegistryObject<EntityType<net.krisplis.koppenclimate.entity.custom.CrabEntity>> CRAB = ENTITY_TYPES.register("crab",
            () -> EntityType.Builder.of(net.krisplis.koppenclimate.entity.custom.CrabEntity::new, MobCategory.CREATURE)
                    .sized(0.5F, 0.3F)
                    .eyeHeight(0.25F)
                    .clientTrackingRange(8)
                    .build("crab"));

    // the cowboy souls haunting the ghost towns at night
    public static final RegistryObject<EntityType<net.krisplis.koppenclimate.entity.custom.CowboySoulEntity>> COWBOY_SOUL = ENTITY_TYPES.register("cowboy_soul",
            () -> EntityType.Builder.<net.krisplis.koppenclimate.entity.custom.CowboySoulEntity>of(net.krisplis.koppenclimate.entity.custom.CowboySoulEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.74F)
                    .fireImmune()
                    .clientTrackingRange(8)
                    .build("cowboy_soul"));
    // a brown eagle of the hot desert, soaring high over the sand
    public static final RegistryObject<EntityType<net.krisplis.koppenclimate.entity.custom.EagleEntity>> EAGLE = ENTITY_TYPES.register("eagle",
            () -> EntityType.Builder.of(net.krisplis.koppenclimate.entity.custom.EagleEntity::new, MobCategory.CREATURE)
                    .sized(0.9F, 0.6F)
                    .eyeHeight(0.4F)
                    .clientTrackingRange(12)
                    .build("eagle"));

    // the rattlesnake of the continental deserts: a viper's size
    public static final RegistryObject<EntityType<net.krisplis.koppenclimate.entity.custom.RattlesnakeEntity>> RATTLESNAKE = ENTITY_TYPES.register("rattlesnake",
            () -> EntityType.Builder.of(net.krisplis.koppenclimate.entity.custom.RattlesnakeEntity::new, MobCategory.CREATURE)
                    .sized(0.55F, 0.3F)
                    .clientTrackingRange(8)
                    .build("rattlesnake"));

    // a termite of the African savanna: a little pale insect, living around its colony's mound
    public static final RegistryObject<EntityType<net.krisplis.koppenclimate.entity.custom.TermiteEntity>> TERMITE = ENTITY_TYPES.register("termite",
            () -> EntityType.Builder.of(net.krisplis.koppenclimate.entity.custom.TermiteEntity::new, MobCategory.CREATURE)
                    .sized(0.35F, 0.2F)
                    .eyeHeight(0.12F)
                    .clientTrackingRange(6)
                    .build("termite"));

    // a small scorpion of the Mediterranean, a bit bigger than a termite: stings what comes too close
    public static final RegistryObject<EntityType<net.krisplis.koppenclimate.entity.custom.ScorpionEntity>> SCORPION = ENTITY_TYPES.register("scorpion",
            () -> EntityType.Builder.of(net.krisplis.koppenclimate.entity.custom.ScorpionEntity::new, MobCategory.CREATURE)
                    .sized(0.5F, 0.3F)
                    .eyeHeight(0.2F)
                    .clientTrackingRange(8)
                    .build("scorpion"));

    // ------------------------------------------------------------------ the African savanna's animals
    public static final RegistryObject<EntityType<net.krisplis.koppenclimate.entity.custom.MambaEntity>> BLACK_MAMBA = ENTITY_TYPES.register("black_mamba",
            () -> EntityType.Builder.of(net.krisplis.koppenclimate.entity.custom.MambaEntity::new, MobCategory.CREATURE)
                    .sized(0.8F, 0.4F).clientTrackingRange(10).build("black_mamba"));
    public static final RegistryObject<EntityType<net.krisplis.koppenclimate.entity.custom.GiraffeEntity>> GIRAFFE = ENTITY_TYPES.register("giraffe",
            () -> EntityType.Builder.of(net.krisplis.koppenclimate.entity.custom.GiraffeEntity::new, MobCategory.CREATURE)
                    .sized(2.0F, 6.4F).eyeHeight(6.0F).clientTrackingRange(10).build("giraffe"));
    public static final RegistryObject<EntityType<net.krisplis.koppenclimate.entity.custom.ElephantEntity>> ELEPHANT = ENTITY_TYPES.register("elephant",
            () -> EntityType.Builder.of(net.krisplis.koppenclimate.entity.custom.ElephantEntity::new, MobCategory.CREATURE)
                    .sized(3.4F, 5.6F).eyeHeight(4.6F).clientTrackingRange(10).build("elephant"));
    public static final RegistryObject<EntityType<net.krisplis.koppenclimate.entity.custom.RhinoEntity>> RHINO = ENTITY_TYPES.register("rhino",
            () -> EntityType.Builder.of(net.krisplis.koppenclimate.entity.custom.RhinoEntity::new, MobCategory.CREATURE)
                    .sized(2.6F, 3.6F).eyeHeight(2.4F).clientTrackingRange(10).build("rhino"));
    public static final RegistryObject<EntityType<net.krisplis.koppenclimate.entity.custom.LionEntity>> LION = ENTITY_TYPES.register("lion",
            () -> EntityType.Builder.of(net.krisplis.koppenclimate.entity.custom.LionEntity::new, MobCategory.CREATURE)
                    .sized(1.1F, 1.3F).clientTrackingRange(10).build("lion"));
    public static final RegistryObject<EntityType<net.krisplis.koppenclimate.entity.custom.ZebraEntity>> ZEBRA = ENTITY_TYPES.register("zebra",
            () -> EntityType.Builder.of(net.krisplis.koppenclimate.entity.custom.ZebraEntity::new, MobCategory.CREATURE)
                    .sized(1.3964844F, 1.5F).eyeHeight(1.425F).clientTrackingRange(10).build("zebra"));
    public static final RegistryObject<EntityType<net.krisplis.koppenclimate.entity.custom.JackalEntity>> JACKAL = ENTITY_TYPES.register("jackal",
            () -> EntityType.Builder.of(net.krisplis.koppenclimate.entity.custom.JackalEntity::new, MobCategory.CREATURE)
                    .sized(0.72F, 1.0F).eyeHeight(0.82F).clientTrackingRange(10).build("jackal"));

    // the tropical forests' pig: reddish-brown with mud-dark spots
    public static final RegistryObject<EntityType<net.krisplis.koppenclimate.entity.custom.WarmPigEntity>> WARM_PIG = ENTITY_TYPES.register("warm_pig",
            () -> EntityType.Builder.of(net.krisplis.koppenclimate.entity.custom.WarmPigEntity::new, MobCategory.CREATURE)
                    .sized(0.9F, 0.9F).passengerAttachments(0.86875F).clientTrackingRange(10).build("warm_pig"));

    // the mod's wooden boats, sized like vanilla's (which wood they are is in the entity: ModBoatWood)
    public static final RegistryObject<EntityType<ModBoat>> BOAT = ENTITY_TYPES.register("boat",
            () -> EntityType.Builder.<ModBoat>of(ModBoat::new, MobCategory.MISC)
                    .sized(1.375F, 0.5625F)
                    .eyeHeight(0.5625F)
                    .clientTrackingRange(10)
                    .build("boat"));

    public static final RegistryObject<EntityType<ModChestBoat>> CHEST_BOAT = ENTITY_TYPES.register("chest_boat",
            () -> EntityType.Builder.<ModChestBoat>of(ModChestBoat::new, MobCategory.MISC)
                    .sized(1.375F, 0.5625F)
                    .eyeHeight(0.5625F)
                    .clientTrackingRange(10)
                    .build("chest_boat"));

    private static <T extends net.minecraft.world.entity.animal.Animal> void registerAnimal(SpawnPlacementRegisterEvent event, EntityType<T> type) {
        event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    /** Whether there's water within this many blocks across (and a couple up or down) of the spot. */
    public static boolean nearWater(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos, int radius) {
        net.minecraft.core.BlockPos.MutableBlockPos p = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dz = -radius; dz <= radius; dz += 2) {
                for (int dy = -2; dy <= 1; dy++) {
                    if (level.getFluidState(p.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz)).is(net.minecraft.tags.FluidTags.WATER)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
        eventBus.addListener(ModEntities::registerAttributes);
        eventBus.addListener(ModEntities::registerSpawnPlacements);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ANACONDA.get(), AnacondaEntity.createAttributes().build());
        event.put(CROCODILE.get(), CrocodileEntity.createAttributes().build());
        event.put(BLACK_PANTHER.get(), BlackPantherEntity.createAttributes().build());
        event.put(PIRANHA.get(), PiranhaEntity.createAttributes().build());
        event.put(COBRA.get(), CobraEntity.createAttributes().build());
        event.put(VIPER.get(), net.krisplis.koppenclimate.entity.custom.ViperEntity.createAttributes().build());
        event.put(SHARK.get(), net.krisplis.koppenclimate.entity.custom.SharkEntity.createAttributes().build());
        event.put(CRAB.get(), net.krisplis.koppenclimate.entity.custom.CrabEntity.createAttributes().build());
        event.put(EAGLE.get(), net.krisplis.koppenclimate.entity.custom.EagleEntity.createAttributes().build());
        event.put(COWBOY_SOUL.get(), net.krisplis.koppenclimate.entity.custom.CowboySoulEntity.createAttributes().build());
        event.put(RATTLESNAKE.get(), net.krisplis.koppenclimate.entity.custom.RattlesnakeEntity.createAttributes().build());
        event.put(TERMITE.get(), net.krisplis.koppenclimate.entity.custom.TermiteEntity.createAttributes().build());
        event.put(SCORPION.get(), net.krisplis.koppenclimate.entity.custom.ScorpionEntity.createAttributes().build());
        event.put(BLACK_MAMBA.get(), net.krisplis.koppenclimate.entity.custom.MambaEntity.createAttributes().build());
        event.put(GIRAFFE.get(), net.krisplis.koppenclimate.entity.custom.GiraffeEntity.createAttributes().build());
        event.put(ELEPHANT.get(), net.krisplis.koppenclimate.entity.custom.ElephantEntity.createAttributes().build());
        event.put(RHINO.get(), net.krisplis.koppenclimate.entity.custom.RhinoEntity.createAttributes().build());
        event.put(LION.get(), net.krisplis.koppenclimate.entity.custom.LionEntity.createAttributes().build());
        event.put(ZEBRA.get(), net.krisplis.koppenclimate.entity.custom.ZebraEntity.createAttributes().build());
        event.put(WARM_PIG.get(), net.minecraft.world.entity.animal.Pig.createAttributes().build());
        event.put(JACKAL.get(), net.krisplis.koppenclimate.entity.custom.JackalEntity.createAttributes().build());
    }

    // Where they may appear: on solid ground. Which biomes is in forge/biome_modifier/*_spawns.json
    private static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(ANACONDA.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        // crocodiles only ever appear near water
        event.register(CROCODILE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> Mob.checkMobSpawnRules(type, level, reason, pos, random) && nearWater(level, pos, 6),
                SpawnPlacementRegisterEvent.Operation.REPLACE);
        // the savanna's animals stand on its grass; rhinos only by its ponds
        for (var type : java.util.List.of(GIRAFFE.get(), ELEPHANT.get(), LION.get(), ZEBRA.get(), JACKAL.get(), WARM_PIG.get())) {
            registerAnimal(event, type);
        }
        event.register(RHINO.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> net.minecraft.world.entity.animal.Animal.checkAnimalSpawnRules(type, level, reason, pos, random)
                        && nearWater(level, pos, 10),
                SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(BLACK_MAMBA.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(TERMITE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(SCORPION.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(BLACK_PANTHER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(COBRA.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(VIPER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        // cod may also spawn in the shallow paddies of the rice terraces, high up the hills (see RiceTerraces)
        event.register(EntityType.COD, net.krisplis.koppenclimate.worldgen.feature.RiceTerracesFeature::codInPaddy,
                SpawnPlacementRegisterEvent.Operation.OR);
        // like vanilla fish: in the water near the surface
        event.register(PIRANHA.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                WaterAnimal::checkSurfaceWaterAnimalSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        // sharks: in the sea near its surface; crabs: on beach sand, like turtles
        event.register(SHARK.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                WaterAnimal::checkSurfaceWaterAnimalSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(CRAB.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.krisplis.koppenclimate.entity.custom.CrabEntity::checkCrabSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(EAGLE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.krisplis.koppenclimate.entity.custom.EagleEntity::checkEagleSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(RATTLESNAKE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(COWBOY_SOUL.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        // cave spiders may also come out by day in the Amazon (see AmazonSpawns)
        // (an extra OR rule keeps vanilla's placement type and heightmap, so it doesn't pass any)
        event.register(EntityType.CAVE_SPIDER, AmazonSpawns::caveSpiderInDaylight, SpawnPlacementRegisterEvent.Operation.OR);
        // bats also fly out over the Mediterranean at night, not only in caves
        event.register(EntityType.BAT, ModEntities::batOverMediterranean, SpawnPlacementRegisterEvent.Operation.OR);
        // the Dry steppe (BSk) is all sand under its grass: its sheep and wolves spawn on that sand
        event.register(EntityType.SHEEP, ModEntities::onSteppeSand, SpawnPlacementRegisterEvent.Operation.OR);
        event.register(EntityType.WOLF, ModEntities::onSteppeSand, SpawnPlacementRegisterEvent.Operation.OR);
    }

    private static final net.minecraft.tags.TagKey<net.minecraft.world.level.biome.Biome> MEDITERRANEAN_BIOMES =
            net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BIOME,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("koppenclimatemod", "is_mediterranean"));

    private static <T extends net.minecraft.world.entity.Entity> boolean onSteppeSand(EntityType<T> type,
                                                net.minecraft.world.level.ServerLevelAccessor level, MobSpawnType reason,
                                                net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource random) {
        return level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.SAND)
                && level.getBiome(pos).is(net.krisplis.koppenclimate.worldgen.biome.ModBiomes.DRY_STEPPE)
                && level.getRawBrightness(pos, 0) > 8;
    }

    private static boolean batOverMediterranean(EntityType<net.minecraft.world.entity.ambient.Bat> type,
                                                net.minecraft.world.level.ServerLevelAccessor level, MobSpawnType reason,
                                                net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource random) {
        var biome = level.getBiome(pos);
        // the Lush desert (BSh) is full of bats, by day as well as by night
        if (net.krisplis.koppenclimate.worldgen.biome.ModBiomes.isLushDesert(biome)) {
            return level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
        }
        if (!biome.is(MEDITERRANEAN_BIOMES)) return false;
        if (!level.getLevel().isNight() || level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos) > 4) return false;
        return level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }
}
