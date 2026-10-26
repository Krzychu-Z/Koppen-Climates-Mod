package net.krisplis.koppenclimate.item;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;

/**
 * Rhino plate armour: thick plates of rhino hide on leather straps. Tougher than iron, a little behind diamond,
 * and it takes some of the knockback. Textures: textures/models/armor/rhino_plate_layer_1/2.png
 */
public final class ModArmorMaterials {
    public static final Holder<ArmorMaterial> RHINO_PLATE = Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL,
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "rhino_plate"),
            new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 3);
                map.put(ArmorItem.Type.LEGGINGS, 5);
                map.put(ArmorItem.Type.CHESTPLATE, 7);
                map.put(ArmorItem.Type.HELMET, 3);
                map.put(ArmorItem.Type.BODY, 7);
            }), 12, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(ModItems.RHINO_PLATE.get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "rhino_plate"))),
                    1.5F, 0.1F));

    private ModArmorMaterials() {
    }
}
