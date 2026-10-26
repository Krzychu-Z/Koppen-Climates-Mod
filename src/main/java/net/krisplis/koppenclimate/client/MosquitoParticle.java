package net.krisplis.koppenclimate.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * A mosquito: a tiny dark speck that hovers in place, darting a little this way and that, for a few seconds.
 */
public class MosquitoParticle extends TextureSheetParticle {
    private final double homeX, homeY, homeZ;

    protected MosquitoParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.homeX = x;
        this.homeY = y;
        this.homeZ = z;
        this.pickSprite(sprites);
        this.quadSize = 0.07F + this.random.nextFloat() * 0.03F;
        this.lifetime = 60 + this.random.nextInt(80);
        this.gravity = 0.0F;
        this.hasPhysics = true;
        this.friction = 0.85F;
        this.xd = this.yd = this.zd = 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.getFluidState(net.minecraft.core.BlockPos.containing(this.x, this.y, this.z)).isEmpty()) {
            this.remove();
            return;
        }
        // erratic buzzing, always pulled back toward where it started
        this.xd += (this.random.nextDouble() - 0.5) * 0.04 + (this.homeX - this.x) * 0.02;
        this.yd += (this.random.nextDouble() - 0.5) * 0.03 + (this.homeY - this.y) * 0.02;
        this.zd += (this.random.nextDouble() - 0.5) * 0.04 + (this.homeZ - this.z) * 0.02;

    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new MosquitoParticle(level, x, y, z, this.sprites);
        }
    }
}
