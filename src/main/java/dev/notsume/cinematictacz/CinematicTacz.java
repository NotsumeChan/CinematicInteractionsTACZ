package dev.notsume.cinematictacz;

import dev.dyslexic.cinematic.api.AddonStations;
import dev.notsume.cinematictacz.tacz.Tacz;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Registers TACZ's tables as Cinematic Interactions add-on stations (both sides). */
public final class CinematicTacz implements ModInitializer {
    public static final String ID = "cinematic_tacz";

    public static ResourceLocation scene(String table) {
        return ResourceLocation.fromNamespaceAndPath(ID, table);
    }

    @Override
    public void onInitialize() {
        // ponytail: each table's front derived from TACZ's getStateForPlacement, checked in game for the default rotations
        for (String table : Tacz.TABLES) {
            AddonStations.register(new AddonStations.Definition(scene(table),
                    ResourceLocation.fromNamespaceAndPath(Tacz.NAMESPACE, table), Tacz.menuClass(),
                    state -> List.of(Tacz.front(state))));
        }
    }
}
