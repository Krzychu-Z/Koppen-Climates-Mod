package net.krisplis.koppenclimate.block;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.block.custom.OxisolGrassBlock;
import net.krisplis.koppenclimate.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, KoppenClimateMod.MOD_ID);

    public static final RegistryObject<Block> OXISOL = registerBlock("oxisol",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_ORANGE)
                    .strength(0.5F)
                    .sound(SoundType.GRAVEL)
                    .instrument(NoteBlockInstrument.BASS)
            )
    );

    public static final RegistryObject<Block> OXISOL_GRASS = registerBlock("oxisol_grass",
            () -> new OxisolGrassBlock(BlockBehaviour.Properties
                    .of().mapColor(MapColor.GRASS)   // like grass
                    .strength(0.6F)
                    .sound(SoundType.GRASS)
                    .randomTicks(),                  // required for spread/decay
                    OXISOL                            // your base oxisol block
            )
    );

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
