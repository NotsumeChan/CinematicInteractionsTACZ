package dev.notsume.cinematictacz;

import dev.dyslexic.cinematic.api.AddonStations;
import dev.notsume.cinematictacz.tacz.Tacz;
import net.minecraft.resources.ResourceLocation;
//? if fabric {
import net.fabricmc.api.ModInitializer;
//?} else if neoforge {
/*import dev.notsume.cinematictacz.client.CinematicTaczClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
*///?} else if forge {
/*import dev.notsume.cinematictacz.client.CinematicTaczClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
*///?}

import java.util.List;

/** Registers TACZ's tables as Cinematic Interactions add-on stations (both sides); on (Neo)Forge it also starts the client. */
//? if fabric {
public final class CinematicTacz implements ModInitializer {
//?} else if neoforge {
/*@Mod(CinematicTacz.ID)
public final class CinematicTacz {
*///?} else if forge {
/*@Mod(CinematicTacz.ID)
public final class CinematicTacz {
*///?}
    public static final String ID = "cinematic_tacz";

    public static ResourceLocation scene(String table) {
        return ResourceLocation.fromNamespaceAndPath(ID, table);
    }

    //? if fabric {
    @Override
    public void onInitialize() {
        registerStations();
    }
    //?} else if neoforge {
    /*public CinematicTacz(IEventBus modBus, ModContainer container) {
        registerStations();
        if (FMLEnvironment.dist == Dist.CLIENT) CinematicTaczClient.init(container);
    }
    *///?} else if forge {
    /*public CinematicTacz() {
        registerStations();
        if (FMLEnvironment.dist == Dist.CLIENT) CinematicTaczClient.init();
    }
    *///?}

    private static void registerStations() {
        // ponytail: each table's front derived from TACZ's getStateForPlacement, checked in game for the default rotations
        for (String table : Tacz.TABLES) {
            AddonStations.register(new AddonStations.Definition(scene(table),
                    ResourceLocation.fromNamespaceAndPath(Tacz.NAMESPACE, table), Tacz.menuClass(),
                    state -> List.of(Tacz.front(state))));
        }
    }
}
