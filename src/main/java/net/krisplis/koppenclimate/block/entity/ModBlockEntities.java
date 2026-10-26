package net.krisplis.koppenclimate.block.entity;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, KoppenClimateMod.MOD_ID);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<ModSignBlockEntity>> SIGN = BLOCK_ENTITIES.register("sign",
            () -> BlockEntityType.Builder.of(ModSignBlockEntity::new,
                    ModBlocks.MAHOGANY_SIGN.get(), ModBlocks.MAHOGANY_WALL_SIGN.get(),
                    ModBlocks.COCONUT_PALM_SIGN.get(), ModBlocks.COCONUT_PALM_WALL_SIGN.get(),
                    ModBlocks.BAOBAB_SIGN.get(), ModBlocks.BAOBAB_WALL_SIGN.get(),
                    ModBlocks.OLIVE_SIGN.get(), ModBlocks.OLIVE_WALL_SIGN.get(),
                    ModBlocks.ITALIAN_PINE_SIGN.get(), ModBlocks.ITALIAN_PINE_WALL_SIGN.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<ModHangingSignBlockEntity>> HANGING_SIGN = BLOCK_ENTITIES.register("hanging_sign",
            () -> BlockEntityType.Builder.of(ModHangingSignBlockEntity::new,
                    ModBlocks.MAHOGANY_HANGING_SIGN.get(), ModBlocks.MAHOGANY_WALL_HANGING_SIGN.get(),
                    ModBlocks.COCONUT_PALM_HANGING_SIGN.get(), ModBlocks.COCONUT_PALM_WALL_HANGING_SIGN.get(),
                    ModBlocks.BAOBAB_HANGING_SIGN.get(), ModBlocks.BAOBAB_WALL_HANGING_SIGN.get(),
                    ModBlocks.OLIVE_HANGING_SIGN.get(), ModBlocks.OLIVE_WALL_HANGING_SIGN.get(),
                    ModBlocks.ITALIAN_PINE_HANGING_SIGN.get(), ModBlocks.ITALIAN_PINE_WALL_HANGING_SIGN.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
