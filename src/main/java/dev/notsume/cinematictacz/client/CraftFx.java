package dev.notsume.cinematictacz.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.HammerModel;
import dev.dyslexic.cinematic.client.Scene;
import dev.notsume.cinematictacz.CinematicTacz;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

/** The crafting effects of the scenes: the forge's hammer, the press's stroke, the screwdriver, smoke and sparks. */
public final class CraftFx {
    private CraftFx() {}

    // ---- the press ----------------------------------------------------------------------------------------

    /** How long a stroke lasts (seconds). */
    private static final float STROKE = 0.24f;

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

    /** How far through a stroke the press of the table at {@code pos} is now, 0 (up) to 1 (down); 0 for every other table. */
    public static float pressStroke(BlockPos pos) {
        if (!pos.equals(pressAt)) return 0;
        double t = Scene.clock() - pressFrom;
        double depth = 0;
        for (double stroke : strokes) {
            double x = (t - stroke) / STROKE + 0.5;
            if (x > 0 && x < 1) depth = Math.max(depth, Math.sin(Math.PI * x));
        }
        return (float) depth;
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
    /** How far the head's lower striking edge lies off the handle's line. */
    static final double HAMMER_ACROSS = 0.375 * HAMMER_SCALE;

    /** The hammer gripped at {@code grip}, its handle along {@code along}, its striking face away from {@code headAxis}. */
    static void drawHammer(PoseStack pose, MultiBufferSource buffers, Vec3 camera, Vec3 grip, Vec3 along, Vec3 headAxis,
                           float size, int light) {
        drawProp(pose, buffers, camera, HammerModel.LOCATION, HAMMER_SCALE * size, HAMMER_GRIP, grip, along, headAxis, light);
    }

    // ---- the screwdriver ----------------------------------------------------------------------------------

    /** The add-on's own prop (models/prop/screwdriver.json, loaded in CinematicTaczClient): tip at y 0, handle up. */
    static final ResourceLocation SCREWDRIVER = ResourceLocation.fromNamespaceAndPath(CinematicTacz.ID, "prop/screwdriver");
    private static final float SCREWDRIVER_SCALE = 0.3f;
    private static final Vec3 SCREWDRIVER_GRIP = new Vec3(0.5, 11.5 / 16, 0.5);
    /** From the tip to the grip, along the shaft. */
    static final double SCREWDRIVER_REACH = SCREWDRIVER_GRIP.y * SCREWDRIVER_SCALE;

    /** The screwdriver gripped at {@code grip}, its shaft along {@code along} (tip to handle), turned by {@code across}. */
    static void drawScrewdriver(PoseStack pose, MultiBufferSource buffers, Vec3 camera, Vec3 grip, Vec3 along, Vec3 across, int light) {
        drawProp(pose, buffers, camera, SCREWDRIVER, SCREWDRIVER_SCALE, SCREWDRIVER_GRIP, grip, along, across, light);
    }

    /** An extra model drawn with its {@code gripPoint} (model units) at {@code grip}, its +y along {@code along}, +x along {@code x}. */
    private static void drawProp(PoseStack pose, MultiBufferSource buffers, Vec3 camera, ResourceLocation location, float scale,
                                 Vec3 gripPoint, Vec3 grip, Vec3 along, Vec3 x, int light) {
        BakedModel model = ((FabricBakedModelManager) Minecraft.getInstance().getModelManager()).getModel(location);
        if (model == null) return;
        Vec3 z = x.cross(along);
        Quaternionf rotation = new Matrix3f(
                (float) x.x, (float) x.y, (float) x.z,
                (float) along.x, (float) along.y, (float) along.z,
                (float) z.x, (float) z.y, (float) z.z).getNormalizedRotation(new Quaternionf());
        pose.pushPose();
        pose.translate(grip.x - camera.x, grip.y - camera.y, grip.z - camera.z);
        pose.mulPose(rotation);
        pose.scale(scale, scale, scale);
        pose.translate(-gripPoint.x, -gripPoint.y, -gripPoint.z);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)), null, model,
                1, 1, 1, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
