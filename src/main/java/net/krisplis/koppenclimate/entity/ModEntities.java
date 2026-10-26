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
        event.put(TERMITE.get(), net.krisplis.koppenclimate.entity.custom.TermiteEntity.createAttributes().build());
        event.put(SCORPION.get(), net.krisplis.koppenclimate.entity.custom.ScorpionEntity.createAttributes().build());
        event.put(BLACK_MAMBA.get(), net.krisplis.koppenclimate.entity.custom.MambaEntity.createAttributes().build());
        event.put(GIRAFFE.get(), net.krisplis.koppenclimate.entity.custom.GiraffeEntity.createAttributes().build());
        event.put(ELEPHANT.get(), net.krisplis.koppenclimate.entity.custom.ElephantEntity.createAttributes().build());
        event.put(RHINO.get(), net.krisplis.koppenclimate.entity.custom.RhinoEntity.createAttributes().build());
        event.put(LION.get(), net.krisplis.koppenclimate.entity.custom.LionEntity.createAttributes().build());
        event.put(ZEBRA.get(), net.krisplis.koppenclimate.entity.custom.ZebraEntity.createAttributes().build());
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
        for (var type : java.util.List.of(GIRAFFE.get(), ELEPHANT.get(), LION.get(), ZEBRA.get(), JACKAL.get())) {
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
        // cod may also spawn in the shallow paddies of the rice terraces, high up the hills (see RiceTerraces)
        event.register(EntityType.COD, net.krisplis.koppenclimate.worldgen.feature.RiceTerracesFeature::codInPaddy,
                SpawnPlacementRegisterEvent.Operation.OR);
        // like vanilla fish: in the water near the surface
        event.register(PIRANHA.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                WaterAnimal::checkSurfaceWaterAnimalSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        // cave spiders may also come out by day in the Amazon (see AmazonSpawns)
        // (an extra OR rule keeps vanilla's placement type and heightmap, so it doesn't pass any)
        event.register(EntityType.CAVE_SPIDER, AmazonSpawns::caveSpiderInDaylight, SpawnPlacementRegisterEvent.Operation.OR);
    }
}
