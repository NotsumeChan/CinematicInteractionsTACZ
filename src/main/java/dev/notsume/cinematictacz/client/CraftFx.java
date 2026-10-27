package dev.notsume.cinematictacz.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.HammerModel;
import dev.dyslexic.cinematic.client.Scene;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

/** The crafting effects of the scenes: the forge's hammer, the press's stroke, smoke and sparks. */
public final class CraftFx {
    private CraftFx() {}

    // ---- the press ----------------------------------------------------------------------------------------

    /** How far the press goes down on a stroke (blocks: 1.5 model pixels) and how long a stroke lasts (seconds). */
    private static final float PRESS_DEPTH = 1.5f / 16, STROKE = 0.24f;

    @Nullable
    private static BlockPos pressAt;
    private static double pressFrom;
    private static double[] strokes = {};

    /** The press of the table at {@code root} strokes at these seconds from now (each centred on its time). */
    static void press(BlockPos root, double[] at) {
        pressAt = root.immutable();
        pressFrom = Scene.clock();
        strokes = at;
    }

    /** How far down the press of the table at {@code pos} is now, in blocks; 0 for every other table. */
    public static float pressDepth(BlockPos pos) {
        if (!pos.equals(pressAt)) return 0;
        double t = Scene.clock() - pressFrom;
        double depth = 0;
        for (double stroke : strokes) {
            double x = (t - stroke) / STROKE + 0.5;
            if (x > 0 && x < 1) depth = Math.max(depth, Math.sin(Math.PI * x));
        }
        return (float) depth * PRESS_DEPTH;
    }

    // ---- particles ----------------------------------------------------------------------------------------

    private static final RandomSource RANDOM = RandomSource.create();

    static void smoke(Vec3 at, int count) {
        burst(ParticleTypes.SMOKE, at, count, 0.06, 0.03);
        burst(ParticleTypes.POOF, at, Math.max(1, count / 3), 0.04, 0.02);
    }

    static void sparks(Vec3 at, int count) {
        burst(ParticleTypes.ELECTRIC_SPARK, at, count, 0.03, 0.12);
        burst(ParticleTypes.SMALL_FLAME, at, Math.max(1, count / 3), 0.02, 0.03);
    }

    private static void burst(ParticleOptions type, Vec3 at, int count, double spread, double speed) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        for (int i = 0; i < count; i++) {
            level.addParticle(type,
                    at.x + RANDOM.triangle(0, spread), at.y + RANDOM.nextDouble() * spread, at.z + RANDOM.triangle(0, spread),
                    RANDOM.triangle(0, speed), RANDOM.nextDouble() * speed, RANDOM.triangle(0, speed));
        }
    }

    // ---- the hammer ---------------------------------------------------------------------------------------

    // Cinematic Interactions' hammer prop, drawn as its HammerModel does (that class keeps its drawing to itself).
    private static final float HAMMER_SCALE = 0.30f;
    private static final Vec3 HAMMER_GRIP = new Vec3(0.5, 0.18, 0.5);
    /** From the grip to the striking edge of the head, along the handle. */
    static final double HAMMER_REACH = (1.125 - HAMMER_GRIP.y) * HAMMER_SCALE;

    /** The hammer gripped at {@code grip}, its handle along {@code along}, its striking face away from {@code headAxis}. */
    static void drawHammer(PoseStack pose, MultiBufferSource buffers, Vec3 camera, Vec3 grip, Vec3 along, Vec3 headAxis, int light) {
        BakedModel model = ((FabricBakedModelManager) Minecraft.getInstance().getModelManager()).getModel(HammerModel.LOCATION);
        if (model == null) return;
        Vec3 z = headAxis.cross(along);
        Quaternionf rotation = new Matrix3f(
                (float) headAxis.x, (float) headAxis.y, (float) headAxis.z,
                (float) along.x, (float) along.y, (float) along.z,
                (float) z.x, (float) z.y, (float) z.z).getNormalizedRotation(new Quaternionf());
        pose.pushPose();
        pose.translate(grip.x - camera.x, grip.y - camera.y, grip.z - camera.z);
        pose.mulPose(rotation);
        pose.scale(HAMMER_SCALE, HAMMER_SCALE, HAMMER_SCALE);
        pose.translate(-HAMMER_GRIP.x, -HAMMER_GRIP.y, -HAMMER_GRIP.z);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), null, model,
                1, 1, 1, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
