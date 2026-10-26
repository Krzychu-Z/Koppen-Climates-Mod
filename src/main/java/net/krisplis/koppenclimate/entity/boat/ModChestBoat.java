package net.krisplis.koppenclimate.entity.boat;

import net.krisplis.koppenclimate.entity.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;

/** A boat with a chest, of one of the mod's woods. */
public class ModChestBoat extends ChestBoat {
    private static final EntityDataAccessor<Integer> DATA_WOOD = SynchedEntityData.defineId(ModChestBoat.class, EntityDataSerializers.INT);

    public ModChestBoat(EntityType<? extends Boat> type, Level level) {
        super(type, level);
    }

    public ModChestBoat(Level level, double x, double y, double z) {
        this(ModEntities.CHEST_BOAT.get(), level);
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
        return this.getWood().item(true);
    }

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
