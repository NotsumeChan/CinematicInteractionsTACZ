package dev.notsume.cinematictacz;

import com.tacz.guns.inventory.GunSmithTableMenu;
import dev.dyslexic.cinematic.api.AddonStations;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Registers TACZ's four gun smith tables as Cinematic Interactions add-on stations (both sides). */
public final class CinematicTacz implements ModInitializer {
    public static final String ID = "cinematic_tacz";

    /**
     * Each table and the side the player works from. Single and tall tables face their placer (A, C); the two-block
     * ones lie across the placer's view with FACING along their length (B), so their front is FACING's clockwise.
     */
    // ponytail: derived from getStateForPlacement, not yet checked in game for every rotation
    public static final Map<String, Function<BlockState, Direction>> TABLES = Map.of(
            "gun_smith_table", state -> facing(state).getClockWise(),
            "workbench_a", CinematicTacz::facing,
            "workbench_b", state -> facing(state).getClockWise(),
            "workbench_c", CinematicTacz::facing);

    public static ResourceLocation scene(String table) {
        return ResourceLocation.fromNamespaceAndPath(ID, table);
    }

    private static Direction facing(BlockState state) {
        return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public void onInitialize() {
        TABLES.forEach((table, front) -> AddonStations.register(new AddonStations.Definition(
                scene(table), ResourceLocation.fromNamespaceAndPath("tacz", table), GunSmithTableMenu.class,
                state -> List.of(front.apply(state)))));
    }
}
