package net.krisplis.koppenclimate.entity;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.AnacondaEntity;
import net.krisplis.koppenclimate.entity.custom.BlackPantherEntity;
import net.krisplis.koppenclimate.entity.custom.CrocodileEntity;
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

    // The hitbox is a short, low box; the model itself is about 5.5 blocks long
    public static final RegistryObject<EntityType<AnacondaEntity>> ANACONDA = ENTITY_TYPES.register("anaconda",
            () -> EntityType.Builder.of(AnacondaEntity::new, MobCategory.CREATURE)
                    .sized(1.2F, 0.5F)
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

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
        eventBus.addListener(ModEntities::registerAttributes);
        eventBus.addListener(ModEntities::registerSpawnPlacements);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ANACONDA.get(), AnacondaEntity.createAttributes().build());
        event.put(CROCODILE.get(), CrocodileEntity.createAttributes().build());
        event.put(BLACK_PANTHER.get(), BlackPantherEntity.createAttributes().build());
    }

    // Where they may appear: on solid ground. Which biomes is in forge/biome_modifier/*_spawns.json
    private static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(ANACONDA.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(CROCODILE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(BLACK_PANTHER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
