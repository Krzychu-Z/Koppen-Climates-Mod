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
    ITALIAN_PINE("italian_pine", ModBlocks.ITALIAN_PINE_PLANKS::get, () -> ModItems.ITALIAN_PINE_BOAT.get(), () -> ModItems.ITALIAN_PINE_CHEST_BOAT.get()),
    FIG("fig", ModBlocks.FIG_PLANKS::get, () -> ModItems.FIG_BOAT.get(), () -> ModItems.FIG_CHEST_BOAT.get()),
    JOSHUA("joshua", ModBlocks.JOSHUA_PLANKS::get, () -> ModItems.JOSHUA_BOAT.get(), () -> ModItems.JOSHUA_CHEST_BOAT.get()),
    DEAD_OAK("dead_oak", ModBlocks.DEAD_OAK_PLANKS::get, () -> ModItems.DEAD_OAK_BOAT.get(), () -> ModItems.DEAD_OAK_CHEST_BOAT.get()),
    EUCALYPTUS("eucalyptus", ModBlocks.EUCALYPTUS_PLANKS::get, () -> ModItems.EUCALYPTUS_BOAT.get(), () -> ModItems.EUCALYPTUS_CHEST_BOAT.get()),
    REDWOOD("redwood", ModBlocks.REDWOOD_PLANKS::get, () -> ModItems.REDWOOD_BOAT.get(), () -> ModItems.REDWOOD_CHEST_BOAT.get()),
    PLATANUS("platanus", ModBlocks.PLATANUS_PLANKS::get, () -> ModItems.PLATANUS_BOAT.get(), () -> ModItems.PLATANUS_CHEST_BOAT.get()),
    CEDAR("cedar", ModBlocks.CEDAR_PLANKS::get, () -> ModItems.CEDAR_BOAT.get(), () -> ModItems.CEDAR_CHEST_BOAT.get()),
    APPLE("apple", ModBlocks.APPLE_PLANKS::get, () -> ModItems.APPLE_BOAT.get(), () -> ModItems.APPLE_CHEST_BOAT.get()),
    APRICOT("apricot", ModBlocks.APRICOT_PLANKS::get, () -> ModItems.APRICOT_BOAT.get(), () -> ModItems.APRICOT_CHEST_BOAT.get()),
    BLACK_POPLAR("black_poplar", ModBlocks.BLACK_POPLAR_PLANKS::get, () -> ModItems.BLACK_POPLAR_BOAT.get(), () -> ModItems.BLACK_POPLAR_CHEST_BOAT.get()),
    CYPRESS("cypress", ModBlocks.CYPRESS_PLANKS::get, () -> ModItems.CYPRESS_BOAT.get(), () -> ModItems.CYPRESS_CHEST_BOAT.get()),
    LEMON("lemon", ModBlocks.LEMON_PLANKS::get, () -> ModItems.LEMON_BOAT.get(), () -> ModItems.LEMON_CHEST_BOAT.get()),
    MAGNOLIA("magnolia", ModBlocks.MAGNOLIA_PLANKS::get, () -> ModItems.MAGNOLIA_BOAT.get(), () -> ModItems.MAGNOLIA_CHEST_BOAT.get()),
    MAPLE("maple", ModBlocks.MAPLE_PLANKS::get, () -> ModItems.MAPLE_BOAT.get(), () -> ModItems.MAPLE_CHEST_BOAT.get()),
    SCOTS_PINE("scots_pine", ModBlocks.SCOTS_PINE_PLANKS::get, () -> ModItems.SCOTS_PINE_BOAT.get(), () -> ModItems.SCOTS_PINE_CHEST_BOAT.get());

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
