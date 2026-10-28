package net.krisplis.koppenclimate.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * A glowworm (firefly) of a Mediterranean night: like a mosquito it hovers about where it appeared, wandering a
 * little this way and that for a few seconds, but it shines a bright yellow-green whatever the darkness, its light
 * swelling and dimming as it goes.
 */
public class GlowwormParticle extends TextureSheetParticle {
    private final double homeX, homeY, homeZ;
    private final float phase;

    protected GlowwormParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.homeX = x;
        this.homeY = y;
        this.homeZ = z;
        this.pickSprite(sprites);
        this.quadSize = 0.011F + this.random.nextFloat() * 0.005F;
        this.lifetime = 80 + this.random.nextInt(100);
        this.gravity = 0.0F;
        this.hasPhysics = true;
        this.friction = 0.88F;
        this.phase = this.random.nextFloat() * Mth.TWO_PI;
        this.xd = this.yd = this.zd = 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.getFluidState(net.minecraft.core.BlockPos.containing(this.x, this.y, this.z)).isEmpty()) {
            this.remove();
            return;
        }
        // a slow, wandering hover, pulled back toward where it appeared
        this.xd += (this.random.nextDouble() - 0.5) * 0.012 + (this.homeX - this.x) * 0.006;
        this.yd += (this.random.nextDouble() - 0.5) * 0.010 + (this.homeY - this.y) * 0.006;
        this.zd += (this.random.nextDouble() - 0.5) * 0.012 + (this.homeZ - this.z) * 0.006;
        // the light pulses
        float pulse = 0.6F + 0.4F * Mth.sin(this.age * 0.18F + this.phase);
        this.rCol = 0.75F + 0.25F * pulse;
        this.gCol = 0.8F + 0.2F * pulse;
        this.bCol = 0.35F * pulse;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
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
            return new GlowwormParticle(level, x, y, z, this.sprites);
        }
    }
}
