package dev.notsume.cinematictacz.tacz;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.block.AbstractGunSmithTableBlock;
import com.tacz.guns.block.GunSmithTableBlockB;
import com.tacz.guns.block.GunSmithTableBlockC;
import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.init.ModRecipe;
import com.tacz.guns.inventory.GunSmithTableMenu;
import com.tacz.guns.resource.filter.RecipeFilter;
//? if >=26.1 {
/*import com.tacz.guns.resource.CommonAssetsManager;
*///?}
import com.tacz.guns.resource.index.CommonBlockIndex;
import com.tacz.guns.resource.pojo.data.block.TabConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
//? if >=26.1 {
/*import net.minecraft.server.level.ServerPlayer;
*///?}
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
//? if >=1.21 {
import net.minecraft.world.item.crafting.RecipeHolder;
//?}
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Everything the add-on takes from TACZ (tested: TaCZ Refabricated 1.21.1-0.7.1-forge1.1.8-hotfix2): the tables, their
 * shapes, menus and recipes. The rest of the add-on knows TACZ only through this class and {@link TaczClient}, so a
 * port with other names or signatures is adapted here alone.
 */
public final class Tacz {
    private Tacz() {}

    public static final String NAMESPACE = "tacz";
    /** The tables by block id path; gun_smith_table is a two-block long table like workbench_b. */
    public static final List<String> TABLES = List.of("gun_smith_table", "workbench_a", "workbench_b", "workbench_c");
    /** The key category of TACZ's own key mappings (shoot, aim ...). */
    public static final String KEY_CATEGORY = "key.category.tacz";

    /** One block (A), two blocks long (B and the gun smith table), two blocks tall (C). */
    public enum Shape { SINGLE, WIDE, TALL }

    public record Input(Ingredient items, int count) {}

    public record Recipe(ResourceLocation id, ItemStack output, List<Input> inputs) {}

    public record Tab(ResourceLocation id, Component name, List<Recipe> recipes) {}

    /** The menu every table opens. */
    @SuppressWarnings("unchecked")
    public static Class<AbstractContainerMenu> menuClass() {
        return (Class<AbstractContainerMenu>) (Class<?>) GunSmithTableMenu.class;
    }

    public static Shape shape(BlockState state) {
        return state.getBlock() instanceof GunSmithTableBlockC ? Shape.TALL
                : state.getBlock() instanceof GunSmithTableBlockB ? Shape.WIDE : Shape.SINGLE;
    }

    /**
     * The side the player works from. Single and tall tables face their placer; the long ones lie across the placer's
     * view with FACING along their length, so their front is FACING's clockwise.
     */
    public static Direction front(BlockState state) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return shape(state) == Shape.WIDE ? facing.getClockWise() : facing;
    }

    /** Along a long table, from its root block towards its other one. */
    public static Direction length(BlockState state) {
        return state.getValue(AbstractGunSmithTableBlock.FACING);
    }

    /** The table's root block, which both blocks of a two-block table lead to; null for anything but a table. */
    @Nullable
    public static BlockPos root(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof AbstractGunSmithTableBlock table ? table.getRootPos(pos, state) : null;
    }

    //? if >=26.1 {
    /*// Opens the table's menu, from its root block whichever block was used, as TACZ's own use does. From 26.1 only the
    // root of a two-block table has a block entity (the menu and its type).
    public static void openMenu(ServerPlayer player, BlockPos pos, BlockState state) {
        BlockPos root = root(player.level(), pos);
        if (root != null && player.level().getBlockEntity(root) instanceof GunSmithTableBlockEntity table) player.openMenu(table);
    }

    *///?}
    /** The table type (a gunpack's block id) the table at {@code root} keeps; null for anything but a table. */
    @Nullable
    public static ResourceLocation keptType(Level level, BlockPos root) {
        if (!(level.getBlockEntity(root) instanceof GunSmithTableBlockEntity table)) return null;
        return table.getId() == null ? DefaultAssets.DEFAULT_BLOCK_ID : table.getId();
    }

    /** The table type an open table menu was opened for; null for any other menu. */
    @Nullable
    public static ResourceLocation menuType(AbstractContainerMenu menu) {
        return menu instanceof GunSmithTableMenu table ? table.getBlockId() : null;
    }

    /** A table type's tabs and their recipes: the ones the server's GunSmithTableMenu.doCraft accepts. */
    public static List<Tab> tabs(@Nullable ResourceLocation type, Level level) {
        boolean everything = DefaultAssets.DEFAULT_BLOCK_ID.equals(type) && !SyncConfig.ENABLE_TABLE_FILTER.get();
        Optional<CommonBlockIndex> index = type == null ? Optional.empty() : TimelessAPI.getCommonBlockIndex(type);
        List<TabConfig> configs = everything ? TabConfig.DEFAULT_TABS : index.map(i -> i.getData().getTabs()).orElse(List.of());
        RecipeFilter filter = everything ? null : index.map(CommonBlockIndex::getFilter).orElse(null);
        // each recipe with its id (1.21 hands them in holders, 1.20.1 knows its own)
        //? if >=26.1 {
        /*// the client has no recipe manager from 26.1: TACZ syncs its table recipes itself, as its own table window reads them
        List<Map.Entry<ResourceLocation, GunSmithTableRecipe>> all = CommonAssetsManager.get().getAllTableRecipes().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().getResult() != null)
                .map(entry -> {
                    GunSmithTableRecipe recipe = new GunSmithTableRecipe(entry.getKey(), entry.getValue());
                    recipe.init();
                    return Map.entry(entry.getKey(), recipe);
                }).toList();
        *///?} else if >=1.21 {
        List<Map.Entry<ResourceLocation, GunSmithTableRecipe>> all = level.getRecipeManager().getAllRecipesFor(ModRecipe.GUN_SMITH_TABLE_CRAFTING)
                .stream().map(holder -> Map.entry(holder.id(), holder.value())).toList();
        //?} else {
        /*List<Map.Entry<ResourceLocation, GunSmithTableRecipe>> all = level.getRecipeManager().getAllRecipesFor(ModRecipe.GUN_SMITH_TABLE_CRAFTING)
                .stream().map(recipe -> Map.entry(recipe.getId(), recipe)).toList();
        *///?}
        List<Tab> tabs = new ArrayList<>();
        for (TabConfig config : configs) {
            List<Recipe> recipes = all.stream()
                    .filter(entry -> config.id().equals(entry.getValue().getTab()))
                    .filter(entry -> filter == null || filter.contains(entry.getKey()))
                    .map(entry -> new Recipe(entry.getKey(), entry.getValue().getOutput(), inputs(entry.getValue())))
                    .toList();
            if (!recipes.isEmpty()) {
                tabs.add(new Tab(config.id(), Component.translatable(config.name()), recipes));
            }
        }
        return tabs;
    }

    private static List<Input> inputs(GunSmithTableRecipe recipe) {
        return recipe.getInputs().stream()
                .map((GunSmithTableIngredient input) -> new Input(input.getIngredient(), input.getCount()))
                .toList();
    }
}
