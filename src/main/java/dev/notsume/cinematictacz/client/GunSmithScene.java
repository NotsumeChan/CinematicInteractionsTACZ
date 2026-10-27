package dev.notsume.cinematictacz.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dyslexic.cinematic.api.client.AddonMenuStation;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.Scene;
import dev.notsume.cinematictacz.mixin.HandsAccessor;
import dev.notsume.cinematictacz.tacz.Tacz;
import dev.notsume.cinematictacz.tacz.TaczClient;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The gun smith table scene: the table's recipes are laid out as a grid of parts, one category at a time. Click a part
 * to choose it (it hovers before the camera with its ingredients), click it again to craft: the ingredients gather into
 * the part while TACZ's own craft packet goes to the server, which takes the materials and hands the item.
 * <p>
 * Each table shape has its own {@link Layout}, fitted to the default gunpack's models: the low tables are worked on
 * their free tabletop, the tall one (two blocks high) on its front, like a tool board.
 */
public final class GunSmithScene extends AddonMenuStation<AbstractContainerMenu> {
    private static final Vec3 UP = new Vec3(0, 1, 0);
    /**
     * The forge (gun smith table): materials on the desk; the hand takes the table's own hammer off its tool wall, three
     * blows, hangs it back; takes the drill off the desk, drills two holes, lays it back; then smoke and fire hide the
     * work (FORGED crafts) and the finished part rises.
     */
    private static final double[] BLOWS = {1.1, 1.5, 1.9}, HOLES = {3.05, 3.45};
    private static final double FORGE_LAND = 0.4, FORGED = 4.05, FORGE_LENGTH = 4.45;
    /** The hammer: the hand reaches for it, takes it, has it on the work (the blows' swing starts), has it back on the wall. */
    private static final double HAMMER_REACH = 0.1, HAMMER_TAKE = 0.4, HAMMER_FROM = 0.7, HAMMER_BACK = 2.3;
    /** The drill: taken, on the first hole, done with the last, back on the desk. */
    private static final double DRILL_TAKE = 2.55, DRILL_FROM = 2.85, DRILL_TO = 3.6, DRILL_BACK = 3.95;
    /** Which way the drill's bit points while drilling: down, away from the player, to the left (not normalised). */
    private static final double[] DRILL_TILT = {0.75, 0.35, 0.5};
    private static final double[] FORGE_BEATS = concat(BLOWS, HOLES, new double[]{FORGED});
    /** The handle's pitch (degrees) at a blow and fully raised. */
    private static final double HAMMER_IMPACT = -12, HAMMER_RAISED = 75;
    /** From the middle of the table hammer's head to its striking face (half its 3 px). */
    private static final double HAMMER_FACE = 1.5 / 16;
    /** How high (blocks) the hand lifts the hammer between blows; it also draws back a third of that. */
    private static final double HAMMER_LIFT = 0.3;
    /** The press (ammo bench): the materials go in, two strokes; the rounds tumble out after, then it crafts. */
    private static final double[] STROKES = {0.6, 1.0};
    private static final double PRESS_LENGTH = 1.7;
    /** The right hand is on the press's lever from this second to this long after the last stroke. */
    private static final double LEVER_FROM = 0.3, LEVER_AFTER = 0.2;
    private static final int ROUNDS = 4;
    /**
     * The attachment bench, a big resin printer: the materials fall into the vat, the player bends down and taps the
     * screen, the build plate goes down into the resin, peels twice, comes back up with the part under it, and the part
     * floats out to the player. Seconds of each beat, then of the whole print.
     */
    private static final double FALL = 0.5, TAP = 0.8, PLATE_DOWN = 1.1, PLATE_DIPPED = 1.7, PEELS_END = 2.5,
            PLATE_UP = 3.1, PRINT_LENGTH = 3.6;
    private static final double[] PRINT_BEATS = {FALL, TAP, PLATE_DOWN, PLATE_DIPPED, PEELS_END, PRINT_LENGTH - 0.15};
    /** How far back up (of its travel) the plate comes on a peel. */
    private static final double PEEL = 0.15;
    /** When the player is bent down to the screen: going down, back up. */
    private static final double BEND_FROM = 0.3, BEND_TO = 1.25;
    /** Guns on the gun smith table are turned so long ones lie side by side on a slant, as on a rack, not end to end. */
    private static final float DIAGONAL = 30;
    /** Seconds the recipe grid takes to shrink away when a craft starts. */
    private static final double GRID_FADE = 0.25;

    /**
     * Where everything goes, in blocks from the middle of the table's footprint at its foot: {@code out} towards the
     * player, {@code up} from the floor. The grid's first row is at (gridOut, gridUp); further rows step away from the
     * player on a top, downwards on an upright board.
     */
    private record Layout(boolean upright, double surface,
                          int cols, int rows, double spacing, float scale, double gridOut, double gridUp,
                          double showOut, double showUp, float showScale,
                          double ingOut, double ingUp, float ingScale, double ingSpacing,
                          double standOut, double camOut, double camUp, float camPitch,
                          double restOut, double restUp, double restAcross) {}

    // ponytail: fitted by eye to tacz_default_gun's models (2026-10-27 screenshots); other gunpacks may need their own
    /** Ammo bench: a press and a tall machine fill the back and right; the free mat is the front half. */
    private static final Layout SINGLE = new Layout(false, 1.0,
            3, 2, 0.2, 0.5f, 0.32, 1.0,
            0.35, 1.48, 0.7f,
            0.42, 1.08, 0.4f, 0.13,
            0.95, 0.95, 1.8, 40,
            0.4, 1.1, 0.3);
    /** Gun smith table: a long desk at 15 px, with trays and a tool wall along its back. */
    private static final Layout WIDE = new Layout(false, 0.94,
            5, 2, 0.34, 0.8f, 0.25, 1.02,
            0.05, 1.5, 1f,
            0.3, 1.36, 0.5f, 0.16,
            0.9, 0.8, 1.64, 36,
            0.32, 1.04, 0.28);
    /** Attachment bench: two blocks tall, worked on its front face like a tool board. */
    private static final Layout TALL = new Layout(true, 1.0,
            3, 2, 0.25, 0.8f, 0.56, 1.4,
            0.86, 1.63, 1f,
            0.56, 0.7, 0.8f, 0.16,
            // eye at standing height, looking down onto the hovering part: lower, the work sat at the top of the view
            1.6, 1.85, 1.55, 15,
            1.0, 0.55, 0.35);

    // ponytail: by the default gunpack's tab ids; a gunpack's own long-gun tabs fall back to 0.7
    /** Long guns are drawn smaller than pistols, so a page of rifles fits the table as one of pistols. */
    private static float tabScale(ResourceLocation tab) {
        return switch (tab.getPath()) {
            case "pistol" -> 1f;
            case "smg" -> 0.8f;
            case "shotgun", "rifle", "misc" -> 0.65f;
            case "sniper", "mg", "rpg" -> 0.5f;
            default -> tab.getNamespace().equals(Tacz.NAMESPACE) ? 1f : 0.7f;
        };
    }

    private final Layout layout;
    /** The middle of the table's footprint, at its foot. */
    private final Vec3 origin;
    /** The table's root block: both blocks of a two-block table lead here. */
    private final BlockPos root;

    private List<Tacz.Tab> tabs = List.of();
    /**
     * The menu the recipes were read from. Moving over from a neighbouring table, the old table's menu is still open for
     * a moment: whichever menu is open now decides.
     */
    private int loadedFor = -1;
    /** Ticks without this table's menu open, and how often it was asked for again. */
    private int waiting, asked;
    private int tab, page, hovered = -1;
    @Nullable
    private Tacz.Recipe selected;
    /** The scale of the tab the chosen part was picked from: it keeps it while the player leafs through others. */
    private float selectedScale = 1;
    /** Recipes on the current page (and the chosen one) the player has the materials for, refreshed every tick. */
    private final Set<ResourceLocation> craftable = new HashSet<>();
    /** How many blows or strokes of the current craft have landed (their sparks, smoke and sound played). */
    private int landed;
    /** A gun was forged: once the move is over the scene closes and the gun is drawn and inspected. */
    private boolean drawAfter;
    /** How the forge's own hammer and drill lie on the table, read when a forging starts; null on a model without them. */
    @Nullable
    private TaczClient.ToolRest hammerRest, drillRest;

    public GunSmithScene(BlockPos pos, BlockState state, Direction side) {
        super(pos, side, Tacz.menuClass(), 0);
        Tacz.Shape shape = Tacz.shape(state);
        layout = switch (shape) {
            case SINGLE -> SINGLE;
            case WIDE -> WIDE;
            case TALL -> TALL;
        };
        BlockPos found = Minecraft.getInstance().level == null ? null : Tacz.root(Minecraft.getInstance().level, pos);
        root = found == null ? pos : found;
        Vec3 foot = Vec3.atBottomCenterOf(root);
        if (shape == Tacz.Shape.WIDE) {
            foot = foot.add(Vec3.atLowerCornerOf(Tacz.length(state).getNormal()).scale(0.5));
        }
        origin = foot;
    }

    // ---- recipes ------------------------------------------------------------------------------------------

    /**
     * Whether the open menu is this table's: its table type matches the one this block keeps. The server may still hold
     * a neighbouring table's menu (tables side by side, a move from one to the other); its recipes are not ours to show,
     * nor to craft from.
     */
    private boolean ours(AbstractContainerMenu menu, ClientLevel level) {
        ResourceLocation kept = Tacz.keptType(level, root);
        return kept != null && kept.equals(Tacz.menuType(menu));
    }

    private void load(AbstractContainerMenu menu, ClientLevel level) {
        tabs = Tacz.tabs(Tacz.menuType(menu), level);
        tab = page = 0;
        selected = null;
        loadedFor = menu.containerId;
    }

    /**
     * Moving quickly from one table to the next, the server can keep the old table's menu and never open this one's.
     * The scene then uses the table as the player would, which opens its menu behind the scene.
     */
    private void askForMenu() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null || mc.player == null) return;
        asked++;
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(center(), side, root, false));
    }

    private int pageSize() {
        return layout.cols() * layout.rows();
    }

    private List<Tacz.Recipe> visible() {
        if (tabs.isEmpty()) return List.of();
        List<Tacz.Recipe> recipes = tabs.get(tab).recipes();
        return recipes.subList(page * pageSize(), Math.min(recipes.size(), (page + 1) * pageSize()));
    }

    private int pages() {
        return tabs.isEmpty() ? 1 : Math.max(1, (tabs.get(tab).recipes().size() + pageSize() - 1) / pageSize());
    }

    private static int count(Tacz.Input input, Inventory inventory) {
        int count = 0;
        for (ItemStack stack : inventory.items) {
            if (!stack.isEmpty() && input.items().test(stack)) count += stack.getCount();
        }
        return count;
    }

    private static boolean canCraft(Tacz.Recipe recipe, Inventory inventory) {
        if (inventory.player.isCreative()) return true;
        for (Tacz.Input input : recipe.inputs()) {
            if (count(input, inventory) < input.count()) return false;
        }
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        LocalPlayer player = Minecraft.getInstance().player;
        AbstractContainerMenu menu = menu();
        ClientLevel level = Minecraft.getInstance().level;
        if (menu != null && menu.containerId != loadedFor && level != null && ours(menu, level)) {
            load(menu, level);
        }
        if (menu != null && menu.containerId == loadedFor) {
            waiting = 0;
        } else if (++waiting % 10 == 0 && asked < 3) {
            askForMenu();
        }
        craftable.clear();
        if (player == null) return;
        List<Tacz.Recipe> check = new ArrayList<>(visible());
        if (selected != null) check.add(selected);
        for (Tacz.Recipe recipe : check) {
            if (canCraft(recipe, player.getInventory())) craftable.add(recipe.id());
        }
    }

    // ---- where things lie ---------------------------------------------------------------------------------

    private Vec3 at(double across, double out, double up) {
        return origin.add(right().scale(across)).add(side().scale(out)).add(0, up, 0);
    }

    /**
     * The middle of the work: the free tabletop, or the middle of the upright board. Cinematic Interactions checks it is
     * in view against the blocks, which are full cubes: never below their top, even where the model's desk is lower.
     */
    @Override
    public Vec3 center() {
        return layout.upright()
                ? at(0, 0.5, layout.gridUp() - layout.spacing() * (layout.rows() - 1) / 2)
                : at(0, 0, Math.max(layout.surface(), 1.0));
    }

    @Override
    public boolean workOnTop() {
        return !layout.upright();
    }

    private Vec3 cell(int index) {
        int column = index % layout.cols(), row = index / layout.cols();
        double across = (column - (layout.cols() - 1) / 2.0) * layout.spacing();
        return layout.upright()
                ? at(across, layout.gridOut(), layout.gridUp() - row * layout.spacing())
                : at(across, layout.gridOut() - row * layout.spacing(), layout.gridUp() + 0.01);
    }

    private Vec3 ingredientPoint(int index, int of) {
        return at((index - (of - 1) / 2.0) * layout.ingSpacing(), layout.ingOut(), layout.ingUp());
    }

    /** Where the chosen part hovers: before the camera, in front of the table's own model and above the grid on screen. */
    private Vec3 showcase() {
        return at(0, layout.showOut(), layout.showUp() + Math.sin(Scene.clock() * 2) * 0.015);
    }

    /** Where the part is forged: the middle of the desk, towards the player. */
    private Vec3 anvil() {
        return at(0, 0.12, layout.surface() + 0.02);
    }

    // ponytail: the press of the default gunpack's ammo bench, placed by eye from screenshots
    /** The press's head, where its smoke puffs, and its mouth, where the materials go in and the rounds come out. */
    private Vec3 pressHead() {
        return at(0.28, 0.02, 1.45);
    }

    private Vec3 pressMouth() {
        return at(0.28, 0.12, 1.08);
    }

    /** Where round {@code k} of the press comes to rest on the mat. */
    private Vec3 roundRest(int k) {
        return at(0.16 - 0.07 * k, 0.32 - 0.02 * k, layout.surface() + 0.01);
    }

    /** The category title: past the grid's last row on a top, over the first row on a board. */
    private Vec3 titlePoint() {
        return layout.upright()
                ? at(0, layout.gridOut(), layout.gridUp() + layout.spacing() * 0.6)
                : at(0, layout.gridOut() - layout.rows() * layout.spacing() + 0.1, layout.gridUp() + 0.05);
    }

    // ---- camera and hands ---------------------------------------------------------------------------------

    @Override
    public List<Vec3> approach(LocalPlayer player) {
        Vec3 stand = at(0, layout.standOut(), 0);
        return List.of(new Vec3(stand.x, player.getY(), stand.z));
    }

    @Override
    public CameraRig.Pose lean() {
        double bend = layout == TALL ? bend(moveTime()) : 0;
        // at the printer the player bends down to its screen and back up
        Vec3 eye = at(0, layout.camOut() - 0.5 * bend, layout.camUp() - 0.65 * bend);
        return new CameraRig.Pose(eye, side.getOpposite().toYRot(), (float) (layout.camPitch() + 30 * bend), 0);
    }

    /** Bending down to the printer's screen takes the camera further from the head than Cinematic Interactions allows. */
    @Override
    public boolean freeCamera() {
        return layout == TALL && moving();
    }

    @Override
    public Vec3 rest(boolean rightHand) {
        return at(rightHand ? layout.restAcross() : -layout.restAcross(), layout.restOut(), layout.restUp());
    }

    @Override
    public void updateCursor(boolean overPanel) {
        hovered = -1;
        aim = null;
        if (overPanel || moving()) return;
        Vec3 hit = layout.upright()
                ? Picking.cursorOnPlane(at(0, layout.gridOut(), 0), side())
                : Picking.cursorOnPlane(origin.y + layout.gridUp() + 0.02);
        if (hit != null) {
            Vec3 offset = hit.subtract(origin);
            double along = layout.upright() ? layout.gridUp() - offset.y : layout.gridOut() - offset.dot(side());
            int column = (int) Math.round(offset.dot(right()) / layout.spacing() + (layout.cols() - 1) / 2.0);
            int row = (int) Math.round(along / layout.spacing());
            int index = row * layout.cols() + column;
            if (column >= 0 && column < layout.cols() && row >= 0 && row < layout.rows() && index < visible().size()) {
                hovered = index;
                aim = cell(index);
                return;
            }
        }
        // Off the grid but on this table (its other block too): still our work, not a neighbouring station to move to.
        aim = onOwnTable();
    }

    @Nullable
    private Vec3 onOwnTable() {
        Vec3[] ray = Picking.cursorRay();
        Minecraft mc = Minecraft.getInstance();
        if (ray == null || mc.level == null || mc.player == null) return null;
        BlockHitResult hit = mc.level.clip(new ClipContext(ray[0], ray[0].add(ray[1].scale(6)), ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, mc.player));
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        return root.equals(Tacz.root(mc.level, hit.getBlockPos())) ? hit.getLocation() : null;
    }

    /** Out of the work: up from a top, towards the player from a board. */
    private Vec3 out() {
        return layout.upright() ? side() : UP;
    }

    @Override
    public Vec3 handTarget(Vec3 target, Vec3 shoulder, boolean carrying, double tap) {
        return target.add(out().scale(carrying ? 0.13 : 0.08));
    }

    @Override
    public Vec3 heldItemPoint(Vec3 hand, Vec3 shoulder) {
        return hand.add(0, -0.07, 0);
    }

    @Override
    protected Vec3 restPoint(int slot) {
        return center();
    }

    // ---- actions ------------------------------------------------------------------------------------------

    @Override
    protected void act(int button, boolean shift) {
        if (moving() || tabs.isEmpty()) return;
        if (button == 1) {
            tab = (tab + 1) % tabs.size();
            page = 0;
            return;
        }
        if (hovered < 0) return;
        Tacz.Recipe recipe = visible().get(hovered);
        if (selected == null || !selected.id().equals(recipe.id())) {
            selected = recipe;
            selectedScale = tabScale(tabs.get(tab).id());
            sound(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.4f, 1.2f);
            return;
        }
        AbstractContainerMenu menu = menu();
        LocalPlayer player = Minecraft.getInstance().player;
        if (menu == null || menu.containerId != loadedFor || player == null || !canCraft(recipe, player.getInventory())) {
            deny();
            return;
        }
        int menuId = menu.containerId;
        landed = 0;
        if (layout == WIDE) {
            sound(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.5f, 0.8f);
            drawAfter = true;
            ClientLevel level = Minecraft.getInstance().level;
            hammerRest = level == null ? null : TaczClient.toolRest(level, root, TaczClient.Tool.HAMMER);
            drillRest = level == null ? null : TaczClient.toolRest(level, root, TaczClient.Tool.DRILL);
            startMove(FORGE_LENGTH, FORGED, () -> {
                AfterCraft.expect(recipe.output());
                TaczClient.craft(recipe.id(), menuId);
            });
        } else if (layout == SINGLE) {
            sound(SoundEvents.SMITHING_TABLE_USE, 0.5f, 1.2f);
            CraftFx.press(root, STROKES);
            // crafted once the rounds lie on the mat, so they do not reach the inventory while still falling
            startMove(PRESS_LENGTH, PRESS_LENGTH, () -> TaczClient.craft(recipe.id(), menuId));
        } else {
            startMove(PRINT_LENGTH, PRINT_LENGTH, () -> TaczClient.craft(recipe.id(), menuId));
        }
    }

    /** Every frame: blows and strokes land on time; when a forging is over, the scene closes for the new gun. */
    @Override
    public void beforeFrame() {
        double t = moveTime();
        if (t >= 0 && layout == TALL) CraftFx.printer(root, (float) plateDrop(t));
        if (t >= 0 && layout == WIDE) {
            Held held = heldTool(t);
            CraftFx.holding(root, held == null ? null : held.tool());
        }
        if (t >= 0) {
            double[] hits = layout == WIDE ? FORGE_BEATS : layout == SINGLE ? STROKES : PRINT_BEATS;
            while (landed < hits.length && t >= hits[landed]) land(landed++);
        } else if (drawAfter) {
            drawAfter = false;
            Scene.leave();
        }
    }

    private void land(int hit) {
        if (layout == WIDE) {
            int hole = hit - BLOWS.length;
            if (hole < 0) {
                CraftFx.sparks(anvil(), 8);
                CraftFx.smoke(anvil(), 3);
                sound(SoundEvents.ANVIL_USE, 0.35f, 1.1f + 0.1f * hit);
            } else if (hole < HOLES.length) {
                CraftFx.sparks(hole(hole), 5);
                CraftFx.smoke(hole(hole), 2);
                sound(SoundEvents.GRINDSTONE_USE, 0.35f, 1.7f);
            } else {
                // the smoke and fire cover the materials turning into the finished part
                CraftFx.smoke(anvil(), 24);
                CraftFx.fire(anvil(), 10);
                sound(SoundEvents.FIRE_EXTINGUISH, 0.4f, 1f);
            }
        } else if (layout == SINGLE) {
            CraftFx.smoke(pressHead(), 6);
            sound(SoundEvents.PISTON_EXTEND, 0.45f, 1.4f);
        } else {
            switch (hit) {
                case 0 -> sound(SoundEvents.GENERIC_SPLASH, 0.2f, 1.6f);
                case 1 -> {
                    sound(SoundEvents.UI_BUTTON_CLICK.value(), 0.3f, 1.6f);
                    sound(SoundEvents.NOTE_BLOCK_BIT.value(), 0.35f, 1.8f);
                }
                case 2, 4 -> sound(SoundEvents.PISTON_CONTRACT, 0.15f, 0.6f);
                case 3 -> sound(SoundEvents.HONEY_BLOCK_PLACE, 0.4f, 1f);
                default -> {
                    CraftFx.smoke(showcase(), 5);
                    sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 1.2f);
                }
            }
        }
    }

    /** How far down the printer's build plate is at {@code t} seconds into a print, 0 (up) to 1 (in the resin). */
    private static double plateDrop(double t) {
        if (t < PLATE_DOWN || t >= PLATE_UP) return 0;
        if (t < PLATE_DIPPED) return ease((t - PLATE_DOWN) / (PLATE_DIPPED - PLATE_DOWN));
        if (t < PEELS_END) return 1 - PEEL * Math.abs(Math.sin(2 * Math.PI * (t - PLATE_DIPPED) / (PEELS_END - PLATE_DIPPED)));
        return 1 - ease((t - PEELS_END) / (PLATE_UP - PEELS_END));
    }

    /** How far the player is bent down to the printer's screen at {@code t}, 0..1. */
    private static double bend(double t) {
        if (t < BEND_FROM || t > BEND_TO) return 0;
        return Math.sin(Math.PI * (t - BEND_FROM) / (BEND_TO - BEND_FROM));
    }

    // ponytail: the default gunpack's printer, measured from its model (resin vat, front screen, build plate); another
    // gunpack's attachment bench gets the same motions at these points
    /** The resin's surface, the middle of the screen on the front, and the build plate's underside when up. */
    private Vec3 vat() {
        return at(0, 0.06, 0.81);
    }

    private Vec3 screen() {
        return at(0, 0.5, 0.31);
    }

    private Vec3 plate(double drop) {
        return at(0, 0.06, 1.28 - TaczClient.PLATE_TRAVEL * drop);
    }

    private static double[] concat(double[]... parts) {
        return Arrays.stream(parts).flatMapToDouble(Arrays::stream).toArray();
    }

    /** Drill hole {@code k}'s point on the work (0 and 1, or between while moving over). */
    private Vec3 hole(double k) {
        return anvil().add(right().scale(-0.06 + 0.12 * k)).add(0, 0.03, 0);
    }

    /** A tool in the hand: where its grip is and how it is turned from the way it lies on the table. */
    private record Held(TaczClient.Tool tool, Vec3 grip, Quaternionf turn) {}

    /** The tool in the right hand at {@code t}, or null when the hand holds none. */
    @Nullable
    private Held heldTool(double t) {
        if (hammerRest != null && t >= HAMMER_TAKE && t <= HAMMER_BACK) {
            return carried(TaczClient.Tool.HAMMER, hammerRest, t, HAMMER_TAKE, HAMMER_FROM, BLOWS[BLOWS.length - 1], HAMMER_BACK);
        }
        if (drillRest != null && t >= DRILL_TAKE && t <= DRILL_BACK) {
            return carried(TaczClient.Tool.DRILL, drillRest, t, DRILL_TAKE, DRILL_FROM, DRILL_TO, DRILL_BACK);
        }
        return null;
    }

    /** A tool taken from its rest at {@code take}, at work from {@code from} to {@code to}, back on its rest at {@code back}. */
    private Held carried(TaczClient.Tool tool, TaczClient.ToolRest rest, double t, double take, double from, double to, double back) {
        if (t >= from && t <= to) return working(tool, t);
        boolean going = t < from;
        Held work = working(tool, going ? from : to);
        // 0 on the table, 1 at work: lifted on an arc and turned on the way
        float u = ease(going ? (t - take) / (from - take) : (back - t) / (back - to));
        Vec3 grip = rest.grip().lerp(work.grip(), u).add(0, 0.12 * Math.sin(Math.PI * u), 0);
        return new Held(tool, grip, new Quaternionf().slerp(work.turn(), u));
    }

    private Held working(TaczClient.Tool tool, double t) {
        if (tool == TaczClient.Tool.HAMMER) {
            Vec3[] hammer = hammer(swing(t));
            return new Held(tool, hammer[0], turn(hammerRest.axis(), hammerRest.across(), hammer[1], hammer[2]));
        }
        // the bit down on a slant, away and to the left, so the body sits back towards the right hand and out of the
        // view; the grip back to the player; in on each hole, over to the next between them
        double push = 0, half = 0.15;
        for (double hole : HOLES) push = Math.max(push, Math.max(0, Math.sin(Math.PI * ((t - hole) / (2 * half) + 0.5))));
        double k = ease(Math.clamp((t - HOLES[0] - half) / (HOLES[1] - HOLES[0] - 2 * half), 0, 1));
        Vec3 tip = hole(k).add(0, 0.05 - 0.045 * push, 0);
        Vec3 bit = UP.scale(-DRILL_TILT[0]).add(side().scale(-DRILL_TILT[1])).add(right().scale(-DRILL_TILT[2]));
        Quaternionf turn = turn(drillRest.axis(), drillRest.across(), bit, side());
        Vector3f reach = turn.transform(drillRest.end().subtract(drillRest.grip()).toVector3f());
        return new Held(tool, tip.subtract(new Vec3(reach)), turn);
    }

    /** The turn taking a tool's axes {@code a}, {@code b} (as it lies) to {@code toA}, {@code toB}. */
    private static Quaternionf turn(Vec3 a, Vec3 b, Vec3 toA, Vec3 toB) {
        return frame(toA, toB).mul(frame(a, b).conjugate());
    }

    /** The rotation taking x to {@code a}, y to {@code b} (squared to {@code a}) and z to their cross. */
    private static Quaternionf frame(Vec3 a, Vec3 b) {
        Vec3 x = a.normalize(), y = b.subtract(x.scale(b.dot(x))).normalize(), z = x.cross(y);
        return new Matrix3f((float) x.x, (float) x.y, (float) x.z, (float) y.x, (float) y.y, (float) y.z,
                (float) z.x, (float) z.y, (float) z.z).getNormalizedRotation(new Quaternionf());
    }

    /** The right hand's target while forging: on a tool, reaching for one, or null outside the work. */
    @Nullable
    private Vec3 forgeHand(double t) {
        Held held = heldTool(t);
        if (held != null) return held.grip();
        if (hammerRest == null) return null;
        if (t >= HAMMER_REACH && t < HAMMER_TAKE) {
            return rest(true).lerp(hammerRest.grip(), ease((t - HAMMER_REACH) / (HAMMER_TAKE - HAMMER_REACH)));
        }
        if (drillRest != null && t > HAMMER_BACK && t < DRILL_TAKE) {
            return hammerRest.grip().lerp(drillRest.grip(), ease((t - HAMMER_BACK) / (DRILL_TAKE - HAMMER_BACK)));
        }
        return null;
    }

    /** The hammer's swing while forging, 0 (on the work) to 1 (raised), or -1 outside the blows. */
    private double swing(double t) {
        if (t < HAMMER_FROM || t > BLOWS[BLOWS.length - 1]) return -1;
        return Math.abs(Math.sin(Math.PI * (t - HAMMER_FROM) / (BLOWS[0] - HAMMER_FROM)));
    }

    /**
     * The table hammer's grip and pose for a swing: grip, handle (grip to head), head's length. As in Cinematic
     * Interactions' anvil, the handle leaves the right hand on a slant, forwards and a little to the left; at a blow it
     * dips just below level and the head's face lands on the work, raised the head turns up round the wrist while the
     * hand lifts HAMMER_LIFT and draws back a third of that.
     */
    private Vec3[] hammer(double swing) {
        Vec3 slant = side().scale(-0.3).add(right().scale(-0.12)).normalize();
        Vec3 along = handle(slant, HAMMER_IMPACT), headAxis = headAxis(slant, HAMMER_IMPACT);
        double reach = hammerRest.end().distanceTo(hammerRest.grip());
        Vec3 grip = anvil().subtract(along.scale(reach)).add(headAxis.scale(HAMMER_FACE))
                .add(UP.scale(HAMMER_LIFT * swing)).add(side().scale(HAMMER_LIFT * 0.35 * swing));
        double angle = HAMMER_IMPACT + (HAMMER_RAISED - HAMMER_IMPACT) * swing;
        return new Vec3[]{grip, handle(slant, angle), headAxis(slant, angle)};
    }

    private static Vec3 handle(Vec3 slant, double degrees) {
        double a = Math.toRadians(degrees);
        return slant.scale(Math.cos(a)).add(UP.scale(Math.sin(a)));
    }

    private static Vec3 headAxis(Vec3 slant, double degrees) {
        double a = Math.toRadians(degrees);
        return UP.scale(Math.cos(a)).subtract(slant.scale(Math.sin(a)));
    }

    /** While forging: the right hand takes and works the tools, the left holds the part down. */
    @Override
    @Nullable
    public Vec3[] handsOverride() {
        double t = moveTime();
        if (layout == SINGLE && t >= LEVER_FROM && t <= STROKES[STROKES.length - 1] + LEVER_AFTER) {
            // the right hand works the press's lever: on its knob, down with it on every stroke
            ClientLevel level = Minecraft.getInstance().level;
            Vec3 knob = level == null ? null : TaczClient.leverKnob(level, root, CraftFx.pressStroke(root));
            return knob == null ? null : new Vec3[]{knob, rest(false)};
        }
        if (layout == TALL && bend(t) > 0) {
            // the right hand reaches the screen as the player bends down, pressing in at the tap
            double press = 0.03 * Math.max(0, 1 - Math.abs(t - TAP) / 0.12);
            Vec3 touch = screen().add(side().scale(0.04 - press));
            return new Vec3[]{rest(true).lerp(touch, Math.min(1, bend(t) * 1.6)), rest(false)};
        }
        Vec3 hand = layout == WIDE ? forgeHand(t) : null;
        if (hand == null) return null;
        return new Vec3[]{hand, anvil().add(right().scale(-0.2)).add(side().scale(0.05)).add(0, 0.04, 0)};
    }

    /** Fast blows and strokes: the hands follow their targets tightly, as Cinematic Interactions' anvil does. */
    @Override
    public double handsStiffness() {
        return moving() ? 55 : 20;
    }

    @Override
    public void scroll(double delta) {
        page = Math.clamp(page + (delta < 0 ? 1 : -1), 0, pages() - 1);
    }

    private void sound(SoundEvent event, float volume, float pitch) {
        ClientLevel level = Minecraft.getInstance().level;
        Vec3 work = center();
        if (level != null) level.playLocalSound(work.x, work.y, work.z, event, SoundSource.BLOCKS, volume, pitch, false);
    }

    // ---- drawing ------------------------------------------------------------------------------------------

    @Override
    public void renderScene(PoseStack pose, MultiBufferSource buffers, Camera camera, int light) {
        Vec3 eye = camera.getPosition();
        // Lying flat on a top; standing up, facing the player, on a board. What hovers is tipped towards the camera.
        float gridTilt = layout.upright() ? 90 : 0, hoverTilt = layout.upright() ? 90 : 70;
        float pageScale = tabs.isEmpty() ? 1 : tabScale(tabs.get(tab).id());
        List<Tacz.Recipe> visible = visible();
        // while crafting, the other recipes shrink off the table and leave the work alone (shrinking, not fading: TACZ's
        // 3D models ignore the fading alpha, so they would linger at full colour and then pop)
        double crafting = moveTime();
        float gridShown = crafting < 0 ? 1f : 1f - (float) Math.min(1, crafting / GRID_FADE);
        for (int i = 0; gridShown > 0 && i < visible.size(); i++) {
            Tacz.Recipe recipe = visible.get(i);
            boolean chosen = selected != null && selected.id().equals(recipe.id());
            float scale = layout.scale() * pageScale * (i == hovered || chosen ? 1.15f : 1f) * ease(gridShown);
            float opacity = chosen || craftable.contains(recipe.id()) ? 1f : 0.4f;
            Vec3 at = cell(i);
            pose.pushPose();
            if (layout == WIDE) {
                // ItemPlacer lays the item at its point: turning the pose round that point turns the item in place
                pose.translate(at.x - eye.x, at.y - eye.y, at.z - eye.z);
                pose.mulPose(Axis.YP.rotationDegrees(DIAGONAL));
                pose.translate(eye.x - at.x, eye.y - at.y, eye.z - at.z);
            }
            ItemPlacer.render(pose, buffers, eye, recipe.output(), at, side, i, scale, gridTilt, light, opacity);
            pose.popPose();
        }
        if (selected == null) return;

        double t = moveTime();
        Vec3 showcase = showcase();
        float showScale = layout.showScale() * selectedScale;
        // the chosen part, and where its materials go while crafting (they slide in and shrink away)
        Vec3 part = showcase, into = showcase;
        float partScale = showScale, partTilt = hoverTilt;
        boolean showPart = true, keepMaterials = false;
        double gather = 0;
        if (t >= 0 && layout == WIDE) {
            // The forge: the preview sinks into the desk and the materials pile there for three blows; the last one
            // hides the work in smoke and the finished part rises out of it before the camera.
            into = anvil();
            gather = Math.min(1, t / FORGE_LAND);
            float lying = layout.scale() * selectedScale;
            double beat = FORGED;
            if (t < FORGE_LAND) {
                float e = ease(gather);
                part = showcase.lerp(anvil(), e);
                partScale = Mth.lerp(e, showScale, 0);
                partTilt = Mth.lerp(e, hoverTilt, 0);
            } else if (t < beat) {
                showPart = false;
                keepMaterials = true;
            } else {
                float e = ease((t - beat) / (FORGE_LENGTH - beat));
                part = anvil().lerp(at(0, layout.camOut() - 0.45, layout.camUp() - 0.2), e);
                partScale = Mth.lerp(e, lying, showScale * 1.2f);
                partTilt = Mth.lerp(e, 0, hoverTilt);
            }
            Held held = heldTool(t);
            ClientLevel level = Minecraft.getInstance().level;
            if (held != null && level != null) {
                // the table's own tool, out of its model (which hides it meanwhile), in the real hand: that springs
                // after its target (forgeHand), so the two never drift apart
                Vec3 hand = HandsAccessor.cinematicTacz$position(true);
                Vec3 grip = (held.tool() == TaczClient.Tool.HAMMER ? hammerRest : drillRest).grip();
                pose.pushPose();
                pose.translate(hand.x - eye.x, hand.y - eye.y, hand.z - eye.z);
                pose.mulPose(held.turn());
                pose.translate(-grip.x, -grip.y, -grip.z);
                TaczClient.drawTool(level, root, held.tool(), pose, buffers, light);
                pose.popPose();
            }
        } else if (t >= 0 && layout == SINGLE) {
            // The press: the materials go into its mouth; after the strokes the rounds tumble out onto the mat.
            into = pressMouth();
            gather = Math.min(1, t / 0.4);
            showPart = false;
            double beat = STROKES[STROKES.length - 1];
            for (int k = 0; k < ROUNDS; k++) {
                double u = (t - beat - 0.08 * k) / 0.35;
                if (u < 0) continue;
                u = Math.min(1, u);
                Vec3 from = pressMouth(), to = roundRest(k);
                Vec3 at = new Vec3(Mth.lerp(u, from.x, to.x), Mth.lerp(u * u, from.y, to.y), Mth.lerp(u, from.z, to.z));
                ItemPlacer.render(pose, buffers, eye, selected.output(), at, side, 300 + k, 0.35f, 0, light);
            }
        } else if (t >= 0) {
            // The printer: the materials and the preview drop into the resin; the part comes up under the build plate,
            // then floats out to where the preview hovered.
            into = vat();
            gather = Math.min(1, t / FALL);
            float hanging = layout.scale() * selectedScale;
            if (t < FALL) {
                float e = (float) (gather * gather);
                part = showcase.lerp(vat(), e);
                partScale = Mth.lerp(e, showScale, 0);
            } else if (t < PEELS_END) {
                showPart = false;
            } else if (t < PLATE_UP) {
                part = plate(plateDrop(t)).add(0, -0.12, 0);
                partScale = hanging;
            } else {
                float e = ease(Math.min(1, (t - PLATE_UP) / (PRINT_LENGTH - 0.15 - PLATE_UP)));
                part = plate(0).add(0, -0.12, 0).lerp(showcase, e);
                partScale = Mth.lerp(e, hanging, showScale);
            }
        }
        if (showPart) {
            ItemPlacer.render(pose, buffers, eye, selected.output(), part, side, 99, partScale, partTilt, light);
        }

        List<Tacz.Input> inputs = selected.inputs();
        for (int i = 0; i < inputs.size(); i++) {
            ItemStack[] options = inputs.get(i).items().getItems();
            if (options.length == 0 || (gather >= 1 && !keepMaterials)) continue;
            ItemStack shown = options[(int) Scene.clock() % options.length];
            Vec3 point = ingredientPoint(i, inputs.size()).lerp(into, gather * gather);
            ItemPlacer.render(pose, buffers, eye, shown, point, side, 200 + i, (float) (layout.ingScale() * (1 - gather * 0.7)), hoverTilt, light);
        }
    }

    private static float ease(double x) {
        return (float) (x * x * (3 - 2 * x));
    }

    @Override
    public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {}

    /** The category and page beside the grid, and under each ingredient what the player has of what it needs. */
    @Override
    public void renderScreen(GuiGraphics graphics, Font font, float alpha) {
        if (alpha < 0.05f) return;
        int a = (int) (alpha * 255) << 24;
        double[] head = Picking.toGui(titlePoint());
        if (head != null) {
            Component title = tabs.isEmpty() ? Component.translatable("cinematic_tacz.empty")
                    : Component.literal("< ").append(tabs.get(tab).name()).append("  " + (page + 1) + "/" + pages() + " >");
            graphics.drawCenteredString(font, title, (int) head[0], (int) head[1], a | 0xF2EAD8);
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (selected == null || player == null || moving()) return;
        List<Tacz.Input> inputs = selected.inputs();
        for (int i = 0; i < inputs.size(); i++) {
            double[] at = Picking.toGui(ingredientPoint(i, inputs.size()).add(0, -0.09, 0));
            if (at == null) continue;
            int have = count(inputs.get(i), player.getInventory()), need = inputs.get(i).count();
            graphics.drawCenteredString(font, have + "/" + need, (int) at[0], (int) at[1], a | (have >= need ? 0x9BE38B : 0xE36B5B));
        }
    }

    @Override
    public Component hint() {
        return Component.translatable("cinematic_tacz.hint");
    }
}
