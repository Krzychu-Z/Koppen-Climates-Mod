package net.krisplis.koppenclimate.block;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

/**
 * The mod's wood types. Signs and fence gates take one; its name (with the mod's namespace) is where the game
 * looks for the sign textures, e.g. textures/entity/signs/mahogany.png. Doors, trapdoors, buttons and pressure
 * plates sound and open like jungle wood.
 */
public final class ModWoodTypes {
    public static final WoodType MAHOGANY = WoodType.register(new WoodType(KoppenClimateMod.MOD_ID + ":mahogany", BlockSetType.JUNGLE));
    public static final WoodType COCONUT_PALM = WoodType.register(new WoodType(KoppenClimateMod.MOD_ID + ":coconut_palm", BlockSetType.JUNGLE));
    public static final WoodType BAOBAB = WoodType.register(new WoodType(KoppenClimateMod.MOD_ID + ":baobab", BlockSetType.ACACIA));
    public static final WoodType OLIVE = WoodType.register(new WoodType(KoppenClimateMod.MOD_ID + ":olive", BlockSetType.ACACIA));
    public static final WoodType ITALIAN_PINE = WoodType.register(new WoodType(KoppenClimateMod.MOD_ID + ":italian_pine", BlockSetType.SPRUCE));
    public static final WoodType FIG = WoodType.register(new WoodType(KoppenClimateMod.MOD_ID + ":fig", BlockSetType.OAK));
    public static final WoodType JOSHUA = WoodType.register(new WoodType(KoppenClimateMod.MOD_ID + ":joshua", BlockSetType.OAK));
    public static final WoodType DEAD_OAK = WoodType.register(new WoodType(KoppenClimateMod.MOD_ID + ":dead_oak", BlockSetType.OAK));
    public static final WoodType EUCALYPTUS = WoodType.register(new WoodType(KoppenClimateMod.MOD_ID + ":eucalyptus", BlockSetType.ACACIA));
    public static final WoodType REDWOOD = WoodType.register(new WoodType(KoppenClimateMod.MOD_ID + ":redwood", BlockSetType.SPRUCE));
    public static final WoodType PLATANUS = WoodType.register(new WoodType(KoppenClimateMod.MOD_ID + ":platanus", BlockSetType.OAK));
    public static final WoodType CEDAR = WoodType.register(new WoodType(KoppenClimateMod.MOD_ID + ":cedar", BlockSetType.SPRUCE));
    public static final WoodType APPLE = WoodType.register(new WoodType(KoppenClimateMod.MOD_ID + ":apple", BlockSetType.OAK));
    public static final WoodType APRICOT = WoodType.register(new WoodType(KoppenClimateMod.MOD_ID + ":apricot", BlockSetType.OAK));
    public static final WoodType BLACK_POPLAR = WoodType.register(new WoodType(KoppenClimateMod.MOD_ID + ":black_poplar", BlockSetType.OAK));
    public static final WoodType CYPRESS = WoodType.register(new WoodType(KoppenClimateMod.MOD_ID + ":cypress", BlockSetType.SPRUCE));
    public static final WoodType LEMON = WoodType.register(new WoodType(KoppenClimateMod.MOD_ID + ":lemon", BlockSetType.OAK));

    private ModWoodTypes() {
    }
}
