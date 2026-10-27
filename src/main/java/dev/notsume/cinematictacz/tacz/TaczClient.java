package dev.notsume.cinematictacz.tacz;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.block.AbstractGunSmithTableBlock;
import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.network.message.ClientMessageCraft;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** The client side of {@link Tacz}: TACZ's craft packet, table window, table models and gun animations. */
public final class TaczClient {
    private TaczClient() {}

    // ponytail: the default gunpack's bone and cubes; a gunpack whose ammo bench differs just gets no press stroke
    /** The reloading press of the ammo bench's model. The whole press is this one bone, its frame included. */
    private static final String PRESS_BONE = "loader";
    /*
     * TACZ makes each turned cube a child part pivoted on it, placed in pixels from the bone's pivot, bedrock
     * (-3, 18.23, 12.67), with y turned over (+y down). The ram: the column's cubes (shaft, cap, collars), all on the
     * column's axis, bedrock (x -3, z 6.5). The lever: its rod and foot turn on its hinge, bedrock (y 10.38, z 9.42), and
     * its knob is the bone's pivot itself.
     */
    private static final float RAM_Z = 6.5f - 12.66936f;
    private static final float HINGE_Y = 18.23148f - 10.37851f, HINGE_Z = 9.41655f - 12.66936f;
    /** How far (blocks) the ram goes down and how far (radians) the lever turns down at the bottom of a stroke. */
    private static final float RAM_TRAVEL = 1.5f / 16, LEVER_SWING = (float) Math.toRadians(30);

    /** Asks the server to craft {@code recipe} in the open table menu {@code menuId}, as TACZ's own Craft button does. */
    public static void craft(ResourceLocation recipe, int menuId) {
        ClientPlayNetworking.send(new ClientMessageCraft(recipe, menuId));
    }

    public static boolean isTableWindow(@Nullable Screen screen) {
        return screen instanceof GunSmithTableScreen;
    }

    /** Plays the inspect animation of the gun in the player's main hand, as TACZ's inspect key does. */
    public static void inspect(LocalPlayer player) {
        IClientPlayerGunOperator.fromLocalPlayer(player).inspect();
    }

    /**
     * Sets how far through a stroke ({@code stroke} 0 up .. 1 down) the press of {@code table}'s model is for the next
     * draw: the ram goes down and the lever turns down with it. The model is shared by every table of its type, so a
     * stroke is set just before one table is drawn and undone (0) right after.
     */
    public static void press(GunSmithTableBlockEntity table, float stroke) {
        BedrockPart press = pressBone(table);
        if (press == null) return;
        float angle = -LEVER_SWING * stroke, cos = Mth.cos(angle), sin = Mth.sin(angle);
        for (BedrockPart part : press.children) {
            if (Math.abs(part.x) > 0.05f) continue;
            if (Math.abs(part.z - RAM_Z) < 0.05f) {
                part.offsetY = RAM_TRAVEL * stroke;
            } else if (isLever(part)) {
                // turned round the hinge: the pivot swings round it and the cube turns with it
                float y = part.y - HINGE_Y, z = part.z - HINGE_Z;
                part.offsetY = (y * cos - z * sin - y) / 16;
                part.offsetZ = (y * sin + z * cos - z) / 16;
                part.xRot = part.getInitRotX() + angle;
            }
        }
    }

    /** Where the knob of the press's lever is in the world, {@code stroke} (0..1) through a stroke; null without one. */
    @Nullable
    public static Vec3 leverKnob(Level level, BlockPos root, float stroke) {
        if (!(level.getBlockEntity(root) instanceof GunSmithTableBlockEntity table)
                || !(table.getBlockState().getBlock() instanceof AbstractGunSmithTableBlock block)) return null;
        BedrockPart press = pressBone(table);
        if (press == null) return null;
        BedrockPart knob = null;
        for (BedrockPart part : press.children) {
            if (Math.abs(part.x) < 0.05f && Math.abs(part.y) < 0.05f && Math.abs(part.z) < 0.05f) knob = part;
        }
        if (knob == null) return null;
        // TACZ's table renderer, then the bones down to the knob, whose pivot is its centre
        PoseStack pose = new PoseStack();
        pose.translate(root.getX() + 0.5, root.getY() + 1.5, root.getZ() + 0.5);
        pose.mulPose(Axis.ZN.rotationDegrees(180));
        pose.mulPose(Axis.YN.rotationDegrees(block.parseRotation(table.getBlockState().getValue(AbstractGunSmithTableBlock.FACING))));
        List<BedrockPart> chain = new ArrayList<>();
        for (BedrockPart part = press; part != null; part = part.getParent()) chain.add(0, part);
        press(table, stroke);
        for (BedrockPart part : chain) part.translateAndRotateAndScale(pose);
        knob.translateAndRotateAndScale(pose);
        press(table, 0);
        Vector3f at = pose.last().pose().transformPosition(new Vector3f());
        return new Vec3(at.x, at.y, at.z);
    }

    private static boolean isLever(BedrockPart part) {
        boolean onHinge = Math.abs(part.y - HINGE_Y) < 0.05f && Math.abs(part.z - HINGE_Z) < 0.05f;
        boolean knob = Math.abs(part.y) < 0.05f && Math.abs(part.z) < 0.05f;
        return onHinge || knob;
    }

    // ponytail: the default gunpack's attachment bench; another model (other bone, other cube count) is left still
    /*
     * The attachment bench is a resin printer. Its bone "crane" holds 16 turned cubes, so 16 child parts in file order:
     * the first 11 are the build plate and its carriage, the last 5 the rails and their top. "workpiece" is the print
     * hanging under the plate.
     */
    private static final String PRINTER_BONE = "crane", PRINT_BONE = "workpiece";
    private static final int PRINTER_PARTS = 16, PLATE_PARTS = 11;
    /** How far (blocks) the build plate goes down: from its rest to just in the resin. */
    public static final float PLATE_TRAVEL = 0.45f;

    /**
     * Sets how far down ({@code drop} 0..1) the build plate of {@code table}'s printer is for the next draw, and hides the
     * model's own print while the scene prints its part. Undone with (0, false) right after the draw, as {@link #press}.
     */
    public static void print(GunSmithTableBlockEntity table, float drop, boolean hidePrint) {
        BedrockModel model = model(table);
        BedrockPart crane = model == null ? null : model.getNode(PRINTER_BONE);
        if (crane == null || crane.children.size() != PRINTER_PARTS) return;
        for (int i = 0; i < PLATE_PARTS; i++) crane.children.get(i).offsetY = PLATE_TRAVEL * drop;
        BedrockPart print = model.getNode(PRINT_BONE);
        if (print != null) print.visible = !hidePrint;
    }

    @Nullable
    private static BedrockPart pressBone(GunSmithTableBlockEntity table) {
        BedrockModel model = model(table);
        return model == null ? null : model.getNode(PRESS_BONE);
    }

    @Nullable
    private static BedrockModel model(GunSmithTableBlockEntity table) {
        ResourceLocation type = table.getId() == null ? DefaultAssets.DEFAULT_BLOCK_ID : table.getId();
        return TimelessAPI.getClientBlockIndex(type).map(index -> index.getModel()).orElse(null);
    }
}
