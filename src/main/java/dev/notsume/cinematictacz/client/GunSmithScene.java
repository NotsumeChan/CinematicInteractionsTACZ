package dev.notsume.cinematictacz.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.block.AbstractGunSmithTableBlock;
import com.tacz.guns.block.GunSmithTableBlockB;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.init.ModRecipe;
import com.tacz.guns.inventory.GunSmithTableMenu;
import com.tacz.guns.network.message.ClientMessageCraft;
import com.tacz.guns.resource.filter.RecipeFilter;
import com.tacz.guns.resource.index.CommonBlockIndex;
import com.tacz.guns.resource.pojo.data.block.TabConfig;
import dev.dyslexic.cinematic.api.client.AddonMenuStation;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.Scene;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The gun smith table scene: the table's recipes lie on its top as a grid of parts, one category at a time. Click a
 * part to choose it (its ingredients line up at the near edge), click it again to craft: the ingredients gather under
 * the hovering result while TACZ's own craft packet goes to the server, which takes the materials and hands the item.
 */
public final class GunSmithScene extends AddonMenuStation<GunSmithTableMenu> {
    private static final int COLS = 4, ROWS = 2, PAGE = COLS * ROWS;
    /** Grid spacing and the two rows on the far half of the top; the ingredients lie on the near half. */
    private static final double SPACING = 0.2, FAR_ROW = -0.05, NEAR_ROW = 0.24, INGREDIENT_SPACING = 0.16;
    private static final double CRAFT_LENGTH = 0.9, CRAFT_BEAT = 0.55;

    private record Tab(Component name, List<RecipeHolder<GunSmithTableRecipe>> recipes) {}

    private final Vec3 top;
    private final List<Tab> tabs = new ArrayList<>();
    private boolean loaded;
    private int tab, page, hovered = -1;
    @Nullable
    private RecipeHolder<GunSmithTableRecipe> selected;
    /** Recipes on the current page (and the chosen one) the player has the materials for, refreshed every tick. */
    private final Set<ResourceLocation> craftable = new HashSet<>();

    public GunSmithScene(BlockPos pos, BlockState state, Direction side) {
        super(pos, side, GunSmithTableMenu.class, 0);
        top = tableTop(pos, state);
    }

    /** The middle of the whole table (both blocks of the two-block ones), on its surface. */
    private static Vec3 tableTop(BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof AbstractGunSmithTableBlock table)) {
            return Vec3.atCenterOf(pos).add(0, 0.5, 0);
        }
        Vec3 center = Vec3.atCenterOf(table.getRootPos(pos, state));
        if (table instanceof GunSmithTableBlockB) {
            Direction length = state.getValue(AbstractGunSmithTableBlock.FACING);
            center = center.add(Vec3.atLowerCornerOf(length.getNormal()).scale(0.5));
        }
        // ponytail: a full block's height for every table; TACZ's models differ (C is two blocks tall), calibrate in game
        return center.add(0, 0.5, 0);
    }

    // ---- recipes ------------------------------------------------------------------------------------------

    /** The same tabs and filter the server's GunSmithTableMenu.doCraft accepts, so nothing listed is refused. */
    private void load(GunSmithTableMenu menu, ClientLevel level) {
        ResourceLocation blockId = menu.getBlockId();
        boolean everything = DefaultAssets.DEFAULT_BLOCK_ID.equals(blockId) && !SyncConfig.ENABLE_TABLE_FILTER.get();
        Optional<CommonBlockIndex> index = blockId == null ? Optional.empty() : TimelessAPI.getCommonBlockIndex(blockId);
        List<TabConfig> configs = everything ? TabConfig.DEFAULT_TABS : index.map(i -> i.getData().getTabs()).orElse(List.of());
        RecipeFilter filter = everything ? null : index.map(CommonBlockIndex::getFilter).orElse(null);
        List<RecipeHolder<GunSmithTableRecipe>> all = level.getRecipeManager().getAllRecipesFor(ModRecipe.GUN_SMITH_TABLE_CRAFTING);
        for (TabConfig config : configs) {
            List<RecipeHolder<GunSmithTableRecipe>> recipes = all.stream()
                    .filter(holder -> config.id().equals(holder.value().getTab()))
                    .filter(holder -> filter == null || filter.contains(holder.id()))
                    .toList();
            if (!recipes.isEmpty()) {
                tabs.add(new Tab(Component.translatable(config.name()), recipes));
            }
        }
        loaded = true;
    }

    private List<RecipeHolder<GunSmithTableRecipe>> visible() {
        if (tabs.isEmpty()) return List.of();
        List<RecipeHolder<GunSmithTableRecipe>> recipes = tabs.get(tab).recipes();
        return recipes.subList(page * PAGE, Math.min(recipes.size(), (page + 1) * PAGE));
    }

    private int pages() {
        return tabs.isEmpty() ? 1 : Math.max(1, (tabs.get(tab).recipes().size() + PAGE - 1) / PAGE);
    }

    private static int count(GunSmithTableIngredient ingredient, Inventory inventory) {
        int count = 0;
        for (ItemStack stack : inventory.items) {
            if (!stack.isEmpty() && ingredient.getIngredient().test(stack)) count += stack.getCount();
        }
        return count;
    }

    private static boolean canCraft(GunSmithTableRecipe recipe, Inventory inventory) {
        if (inventory.player.isCreative()) return true;
        for (GunSmithTableIngredient ingredient : recipe.getInputs()) {
            if (count(ingredient, inventory) < ingredient.getCount()) return false;
        }
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        LocalPlayer player = Minecraft.getInstance().player;
        GunSmithTableMenu menu = menu();
        if (!loaded && menu != null && Minecraft.getInstance().level != null) {
            load(menu, Minecraft.getInstance().level);
        }
        craftable.clear();
        if (player == null) return;
        List<RecipeHolder<GunSmithTableRecipe>> check = new ArrayList<>(visible());
        if (selected != null) check.add(selected);
        for (RecipeHolder<GunSmithTableRecipe> holder : check) {
            if (canCraft(holder.value(), player.getInventory())) craftable.add(holder.id());
        }
    }

    // ---- where things lie ---------------------------------------------------------------------------------

    @Override
    public Vec3 center() {
        return top;
    }

    private Vec3 cell(int index) {
        int column = index % COLS, row = index / COLS;
        return top.add(right().scale((column - (COLS - 1) / 2.0) * SPACING))
                .add(side().scale(FAR_ROW - row * SPACING)).add(0, 0.01, 0);
    }

    private Vec3 ingredientPoint(int index, int of) {
        return top.add(right().scale((index - (of - 1) / 2.0) * INGREDIENT_SPACING))
                .add(side().scale(NEAR_ROW)).add(0, 0.01, 0);
    }

    /** Where the chosen part hovers, over the middle of the top. */
    private Vec3 showcase() {
        return top.add(0, 0.28 + Math.sin(Scene.clock() * 2) * 0.015, 0);
    }

    // ---- camera and hands ---------------------------------------------------------------------------------

    @Override
    public List<Vec3> approach(LocalPlayer player) {
        Vec3 stand = top.add(side().scale(0.9));
        return List.of(new Vec3(stand.x, player.getY(), stand.z));
    }

    @Override
    public CameraRig.Pose lean() {
        return new CameraRig.Pose(top.add(side().scale(0.8)).add(0, 0.7, 0), side.getOpposite().toYRot(), 36, 0);
    }

    @Override
    public Vec3 rest(boolean rightHand) {
        return top.add(side().scale(0.32)).add(right().scale(rightHand ? 0.28 : -0.28)).add(0, 0.1, 0);
    }

    @Override
    public void updateCursor(boolean overPanel) {
        hovered = -1;
        aim = null;
        if (overPanel || moving()) return;
        Vec3 hit = Picking.cursorOnPlane(top.y + 0.02);
        if (hit == null) return;
        Vec3 offset = hit.subtract(top);
        int column = (int) Math.round(offset.dot(right()) / SPACING + (COLS - 1) / 2.0);
        int row = (int) Math.round((FAR_ROW - offset.dot(side())) / SPACING);
        int index = row * COLS + column;
        if (column >= 0 && column < COLS && row >= 0 && row < ROWS && index < visible().size()) {
            hovered = index;
            aim = cell(index);
        }
    }

    @Override
    public Vec3 handTarget(Vec3 target, Vec3 shoulder, boolean carrying, double tap) {
        return target.add(0, carrying ? 0.13 : 0.08, 0);
    }

    @Override
    public Vec3 heldItemPoint(Vec3 hand, Vec3 shoulder) {
        return hand.add(0, -0.07, 0);
    }

    @Override
    protected Vec3 restPoint(int slot) {
        return top;
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
        RecipeHolder<GunSmithTableRecipe> recipe = visible().get(hovered);
        if (selected == null || !selected.id().equals(recipe.id())) {
            selected = recipe;
            sound(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.4f, 1.2f);
            return;
        }
        GunSmithTableMenu menu = menu();
        LocalPlayer player = Minecraft.getInstance().player;
        if (menu == null || player == null || !canCraft(recipe.value(), player.getInventory())) {
            deny();
            return;
        }
        int menuId = menu.containerId;
        sound(SoundEvents.SMITHING_TABLE_USE, 0.5f, 1f);
        startMove(CRAFT_LENGTH, CRAFT_BEAT, () -> ClientPlayNetworking.send(new ClientMessageCraft(recipe.id(), menuId)));
    }

    @Override
    public void scroll(double delta) {
        page = Math.clamp(page + (delta < 0 ? 1 : -1), 0, pages() - 1);
    }

    private void sound(net.minecraft.sounds.SoundEvent event, float volume, float pitch) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) level.playLocalSound(top.x, top.y, top.z, event, SoundSource.BLOCKS, volume, pitch, false);
    }

    // ---- drawing ------------------------------------------------------------------------------------------

    @Override
    public void renderScene(PoseStack pose, MultiBufferSource buffers, Camera camera, int light) {
        Vec3 eye = camera.getPosition();
        List<RecipeHolder<GunSmithTableRecipe>> visible = visible();
        for (int i = 0; i < visible.size(); i++) {
            RecipeHolder<GunSmithTableRecipe> holder = visible.get(i);
            if (holder == selected) continue;
            float scale = i == hovered ? 1.15f : 1f;
            float opacity = craftable.contains(holder.id()) ? 1f : 0.4f;
            ItemPlacer.render(pose, buffers, eye, holder.value().getOutput(), cell(i), side, i, scale, 0, light, opacity);
        }
        if (selected == null) return;

        // While crafting: the ingredients slide in under the part and shrink away, then the part pops.
        double t = moveTime();
        double gather = t < 0 ? 0 : Math.min(1, t / CRAFT_BEAT);
        float pop = t < CRAFT_BEAT ? 1f : (float) (1 + 0.3 * Math.sin((t - CRAFT_BEAT) / (CRAFT_LENGTH - CRAFT_BEAT) * Math.PI));
        ItemPlacer.render(pose, buffers, eye, selected.value().getOutput(), showcase(), side, 99, 1.6f * pop, 70, light);

        List<GunSmithTableIngredient> inputs = selected.value().getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            ItemStack[] options = inputs.get(i).getIngredient().getItems();
            if (options.length == 0 || gather >= 1) continue;
            ItemStack shown = options[(int) (Scene.clock()) % options.length];
            Vec3 at = ingredientPoint(i, inputs.size()).lerp(showcase().add(0, -0.1, 0), gather * gather);
            ItemPlacer.render(pose, buffers, eye, shown, at, side, 200 + i, (float) (1 - gather * 0.7), 0, light);
        }
    }

    @Override
    public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {}

    /** The category and page over the far edge, and under each ingredient what the player has of what it needs. */
    @Override
    public void renderScreen(GuiGraphics graphics, Font font, float alpha) {
        if (alpha < 0.05f) return;
        int a = (int) (alpha * 255) << 24;
        double[] head = Picking.toGui(top.add(side().scale(FAR_ROW - ROWS * SPACING)).add(0, 0.05, 0));
        if (head != null) {
            Component title = tabs.isEmpty() ? Component.translatable("cinematic_tacz.empty")
                    : Component.literal("< ").append(tabs.get(tab).name()).append("  " + (page + 1) + "/" + pages() + " >");
            graphics.drawCenteredString(font, title, (int) head[0], (int) head[1], a | 0xF2EAD8);
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (selected == null || player == null || moving()) return;
        List<GunSmithTableIngredient> inputs = selected.value().getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            double[] at = Picking.toGui(ingredientPoint(i, inputs.size()).add(side().scale(0.07)));
            if (at == null) continue;
            int have = count(inputs.get(i), player.getInventory()), need = inputs.get(i).getCount();
            graphics.drawCenteredString(font, have + "/" + need, (int) at[0], (int) at[1], a | (have >= need ? 0x9BE38B : 0xE36B5B));
        }
    }

    @Override
    public Component hint() {
        return Component.translatable("cinematic_tacz.hint");
    }
}
