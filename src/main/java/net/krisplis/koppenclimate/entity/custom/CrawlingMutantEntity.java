package net.krisplis.koppenclimate.entity.custom;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/**
 * The crawling mutant, of the abandoned Soviet estate (ChernobylPiece): a mutant zombie whose legs no longer carry it,
 * dragging itself along the ground on its arms - flat to the floor (it slips under anything a block high), and
 * horribly fast, far faster than a running player. Weaker than the others, but there's no outrunning it. Like them it
 * doesn't burn in the sun, its blows poison, and it glows.
 */
public class CrawlingMutantEntity extends MutantZombieEntity {
    private static final EntityDimensions CRAWLING = EntityDimensions.scalable(0.8F, 0.6F).withEyeHeight(0.4F);

    public CrawlingMutantEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return MutantZombieEntity.createAttributes().add(Attributes.MAX_HEALTH, 18.0).add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MOVEMENT_SPEED, 0.46).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public void tick() {
        // always flat on the ground: the crawl (the game draws a mob lying in the swimming pose crawling, as a player
        // crawls through a gap a block high)
        if (this.getPose() != Pose.SWIMMING && !this.isDeadOrDying()) this.setPose(Pose.SWIMMING);
        super.tick();
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return CRAWLING.scale(this.getAgeScale());
    }
}
