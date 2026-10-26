package net.krisplis.koppenclimate.entity.boat;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/** The mod's boat woods. Vanilla's Boat.Type can't be extended, so the mod's boats carry this instead. */
public enum ModBoatWood {
    MAHOGANY("mahogany", ModBlocks.MAHOGANY_PLANKS::get, () -> ModItems.MAHOGANY_BOAT.get(), () -> ModItems.MAHOGANY_CHEST_BOAT.get()),
    COCONUT_PALM("coconut_palm", ModBlocks.COCONUT_PALM_PLANKS::get, () -> ModItems.COCONUT_PALM_BOAT.get(), () -> ModItems.COCONUT_PALM_CHEST_BOAT.get()),
    BAOBAB("baobab", ModBlocks.BAOBAB_PLANKS::get, () -> ModItems.BAOBAB_BOAT.get(), () -> ModItems.BAOBAB_CHEST_BOAT.get()),
    OLIVE("olive", ModBlocks.OLIVE_PLANKS::get, () -> ModItems.OLIVE_BOAT.get(), () -> ModItems.OLIVE_CHEST_BOAT.get()),
    ITALIAN_PINE("italian_pine", ModBlocks.ITALIAN_PINE_PLANKS::get, () -> ModItems.ITALIAN_PINE_BOAT.get(), () -> ModItems.ITALIAN_PINE_CHEST_BOAT.get());

    private final String name;
    private final Supplier<Block> planks;
    private final Supplier<Item> boat;
    private final Supplier<Item> chestBoat;

    ModBoatWood(String name, Supplier<Block> planks, Supplier<Item> boat, Supplier<Item> chestBoat) {
        this.name = name;
        this.planks = planks;
        this.boat = boat;
        this.chestBoat = chestBoat;
    }

    public String getName() {
        return this.name;
    }

    public Block planks() {
        return this.planks.get();
    }

    public Item item(boolean chest) {
        return (chest ? this.chestBoat : this.boat).get();
    }

    public ResourceLocation texture(boolean chest) {
        return ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID,
                "textures/entity/" + (chest ? "chest_boat/" : "boat/") + this.name + ".png");
    }

    public static ModBoatWood byId(int id) {
        ModBoatWood[] values = values();
        return id >= 0 && id < values.length ? values[id] : MAHOGANY;
    }

    public static ModBoatWood byName(String name) {
        for (ModBoatWood wood : values()) {
            if (wood.name.equals(name)) return wood;
        }
        return MAHOGANY;
    }
}
