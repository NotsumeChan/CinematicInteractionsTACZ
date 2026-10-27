package dev.notsume.cinematictacz.client;

import dev.dyslexic.cinematic.client.Scene;
import dev.notsume.cinematictacz.tacz.TaczClient;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The crafting effects of the scenes, and what of them the table renderer must know: the forge's tool in the hand, the
 * press's stroke, the printer's plate; smoke, sparks and fire.
 */
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

    // ---- the printer --------------------------------------------------------------------------------------

    @Nullable
    private static BlockPos printAt;
    private static float plate;
    private static long printSeen;

    /** The scene's print on the table at {@code root} has its build plate {@code drop} (0..1) down this frame. */
    static void printer(BlockPos root, float drop) {
        printAt = root.immutable();
        plate = drop;
        printSeen = Util.getMillis();
    }

    /**
     * How far down the build plate of the table at {@code pos} is now (0..1), or -1 when it is not printing. A print
     * the scene stopped telling about (it ended, or closed midway) is over.
     */
    public static float plateDrop(BlockPos pos) {
        return pos.equals(printAt) && Util.getMillis() - printSeen < 200 ? plate : -1;
    }

    // ---- the forge's tools --------------------------------------------------------------------------------

    @Nullable
    private static BlockPos toolAt;
    @Nullable
    private static TaczClient.Tool tool;
    private static long toolSeen;

    /** This frame the hand holds {@code held} (null: none) of the table at {@code root}, out of the table's model. */
    static void holding(BlockPos root, @Nullable TaczClient.Tool held) {
        toolAt = root.immutable();
        tool = held;
        toolSeen = Util.getMillis();
    }

    /** The tool of the table at {@code pos} that is in the hand now, or null; as {@link #plateDrop}, a stale one is back. */
    @Nullable
    public static TaczClient.Tool held(BlockPos pos) {
        return pos.equals(toolAt) && Util.getMillis() - toolSeen < 200 ? tool : null;
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

    static void fire(Vec3 at, int count) {
        burst(ParticleTypes.FLAME, at, count, 0.08, 0.04);
        burst(ParticleTypes.LAVA, at, Math.max(1, count / 4), 0.04, 0);
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
}
