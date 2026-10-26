package net.krisplis.koppenclimate.entity.boat;

import net.krisplis.koppenclimate.entity.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;

/** A plain boat of one of the mod's woods: a vanilla boat that drops, and looks like, its own wood. */
public class ModBoat extends Boat {
    private static final EntityDataAccessor<Integer> DATA_WOOD = SynchedEntityData.defineId(ModBoat.class, EntityDataSerializers.INT);

    public ModBoat(EntityType<? extends Boat> type, Level level) {
        super(type, level);
    }

    public ModBoat(Level level, double x, double y, double z) {
        this(ModEntities.BOAT.get(), level);
        this.setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_WOOD, ModBoatWood.MAHOGANY.ordinal());
    }

    public ModBoatWood getWood() {
        return ModBoatWood.byId(this.entityData.get(DATA_WOOD));
    }

    public void setWood(ModBoatWood wood) {
        this.entityData.set(DATA_WOOD, wood.ordinal());
    }

    @Override
    public Item getDropItem() {
        return this.getWood().item(false);
    }

    // a boat smashed by a fall drops planks of its variant, which for this boat would be oak
    @Nullable
    @Override
    public ItemEntity spawnAtLocation(ItemLike item) {
        return super.spawnAtLocation(item == Blocks.OAK_PLANKS ? this.getWood().planks() : item);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Wood", this.getWood().getName());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Wood", 8)) {
            this.setWood(ModBoatWood.byName(tag.getString("Wood")));
        }
    }
}
