package net.krisplis.koppenclimate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.item.custom.Kar98kItem;
import net.krisplis.koppenclimate.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The Kar98k as the player handles it: the left button fires (the attack is cancelled and the trigger sent to the
 * server, with the shooter's own puff of smoke at the muzzle); raised with the right button (and left raised till
 * the next right click), it comes up to the eye with its iron sights lined up exactly on the middle of the screen -
 * the front post's top flush with the rear notch's, on the crosshair - and the view narrows a little (no telescope:
 * a mild 0.8).
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Kar98kClient {
    private static final float AIM_FOV = 0.8F, SCOPE_FOV = 0.28F, HIGH_SCOPE_FOV = 0.12F;
    // (see applyForgeHandTransform)
    private static final float SIGHT_X = -0.02205F, SIGHT_Y = -0.15680F, SIGHT_Z = 0.04033F;

    private Kar98kClient() {
    }

    @SubscribeEvent
    public static void onClick(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !(player.getMainHandItem().getItem() instanceof Kar98kItem rifle)) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        // one shot to a press of the button (holding it down on a block would otherwise go on firing)
        if (held) return;
        held = true;
        if (player.getCooldowns().isOnCooldown(rifle)) return;
        ModNetwork.fireRifle();
        // the kick: the muzzle (and with it the view, and the scope) jumps up - hard for the Mosin, less for the
        // Kar98k, a little for the AK-47 - if there's a bullet to fire
        if (player.getAbilities().instabuild || player.getInventory().contains(new ItemStack(net.krisplis.koppenclimate.item.ModItems.BULLET.get()))) {
            float kick = rifle instanceof net.krisplis.koppenclimate.item.custom.MosinItem ? 7.0F
                    : rifle instanceof net.krisplis.koppenclimate.item.custom.Ak47Item ? 1.4F : 4.5F;
            recoil += kick;
            recoilYaw += (player.getRandom().nextFloat() - 0.5F) * kick * 0.3F;
        }
        Vec3 at = mc.options.getCameraType().isFirstPerson() ? Kar98kItem.muzzle(player, Kar98kItem.aiming(player))
                : Kar98kItem.muzzle(player, false);
        if (mc.level != null) {
            mc.level.addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z, 0.0, 0.01, 0.0);
            mc.level.addParticle(ParticleTypes.SMOKE, at.x, at.y + 0.03, at.z, 0.0, 0.015, 0.0);
        }
    }

    private static boolean held;
    // the kick still to come (degrees), worked off over a few frames
    private static float recoil, recoilYaw;
    private static long lastFrame;

    @SubscribeEvent
    public static void onFrame(net.minecraftforge.event.TickEvent.RenderTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.START) return;
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 0.0F : Math.min((now - lastFrame) / 1.0E9F, 0.1F);
        lastFrame = now;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || recoil <= 0.01F && Math.abs(recoilYaw) <= 0.01F) {
            recoil = 0.0F;
            recoilYaw = 0.0F;
            return;
        }
        // most of it in the first few hundredths of a second: a sharp jump, not a drift
        float part = Math.min(1.0F, dt * 22.0F);
        float up = recoil * part, side = recoilYaw * part;
        recoil -= up;
        recoilYaw -= side;
        player.setXRot(net.minecraft.util.Mth.clamp(player.getXRot() - up, -90.0F, 90.0F));
        player.xRotO = player.getXRot();
        player.setYRot(player.getYRot() + side);
        player.yRotO = player.getYRot();
    }

    @SubscribeEvent
    public static void onTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END && !Minecraft.getInstance().options.keyAttack.isDown()) held = false;
    }

    // down the iron sights: the view narrows a little
    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        // (the Mosin's telescope: some three and a half times, in place of a spyglass's ten)
        if (net.krisplis.koppenclimate.item.custom.MosinItem.scoped(event.getPlayer())) {
            // (and turned up: some eight times)
            event.setNewFovModifier(net.krisplis.koppenclimate.item.custom.MosinItem.zoom(event.getPlayer().getMainHandItem()) >= 2
                    ? HIGH_SCOPE_FOV : SCOPE_FOV);
        }
        else if (Kar98kItem.aiming(event.getPlayer())) event.setNewFovModifier(event.getNewFovModifier() * AIM_FOV);
    }

    public static IClientItemExtensions extensions() {
        return new IClientItemExtensions() {
            /*
             * In first person, aimed: up to the eye, the sight line on the middle of the view. The model's own first
             * person turn (kar98k_3d: x 4, y 94 degrees) is undone to a plain 90 about y, so the barrel runs straight
             * away from the eye; then the rear sight's notch and the front post (both at y 11.1, z 8 in the model) sit
             * exactly on the line through the middle of the screen, the notch a fifth of a block in front of the eye.
             * (Worked out from the model's transform: translation (0, 1, -5)/16, scale 0.6, the model's -0.5 offset.)
             */
            @Override
            public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                                   float partialTick, float equipProcess, float swingProcess) {
                if (arm != player.getMainArm() || !Kar98kItem.isAimed(stack)) return false;
                poseStack.translate(SIGHT_X, SIGHT_Y + equipProcess * -0.6F, SIGHT_Z);
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-4.0F));
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-4.0F));
                return true;
            }
        };
    }
}
