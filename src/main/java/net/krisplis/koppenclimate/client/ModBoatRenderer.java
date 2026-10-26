package net.krisplis.koppenclimate.client;

import com.mojang.datafixers.util.Pair;
import net.krisplis.koppenclimate.entity.boat.ModBoat;
import net.krisplis.koppenclimate.entity.boat.ModBoatWood;
import net.krisplis.koppenclimate.entity.boat.ModChestBoat;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.ChestBoatModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Boat;

import java.util.EnumMap;
import java.util.Map;

/** Draws the mod's boats with vanilla's boat model and the wood's own texture. */
public class ModBoatRenderer extends BoatRenderer {
    private final Map<ModBoatWood, Pair<ResourceLocation, ListModel<Boat>>> resources = new EnumMap<>(ModBoatWood.class);

    public ModBoatRenderer(EntityRendererProvider.Context context, boolean chest) {
        super(context, chest);
        for (ModBoatWood wood : ModBoatWood.values()) {
            ListModel<Boat> model = chest
                    ? new ChestBoatModel(context.bakeLayer(ModelLayers.createChestBoatModelName(Boat.Type.OAK)))
                    : new BoatModel(context.bakeLayer(ModelLayers.createBoatModelName(Boat.Type.OAK)));
            this.resources.put(wood, Pair.of(wood.texture(chest), model));
        }
    }

    @Override
    public Pair<ResourceLocation, ListModel<Boat>> getModelWithLocation(Boat boat) {
        ModBoatWood wood = boat instanceof ModChestBoat chestBoat ? chestBoat.getWood()
                : boat instanceof ModBoat plain ? plain.getWood() : ModBoatWood.MAHOGANY;
        return this.resources.get(wood);
    }
}
