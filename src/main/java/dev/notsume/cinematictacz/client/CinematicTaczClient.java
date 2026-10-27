package dev.notsume.cinematictacz.client;

import dev.dyslexic.cinematic.api.client.AddonScenes;
import dev.notsume.cinematictacz.CinematicTacz;
import dev.notsume.cinematictacz.tacz.Tacz;
//? if fabric {
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//?} else if neoforge {
/*import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
*///?} else if forge {
/*import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
*///?}

/** The client's start: a scene per table, the settings, and the after-craft tick. */
//? if fabric {
public final class CinematicTaczClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Settings.load();
        registerScenes();
        ClientTickEvents.END_CLIENT_TICK.register(AfterCraft::tick);
    }
//?} else if neoforge {
/*public final class CinematicTaczClient {
    /^* From the mod's constructor, on the client only. ^/
    public static void init(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, Settings.SPEC);
        // the mods list's Config button
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new SettingsScreen(parent));
        registerScenes();
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> AfterCraft.tick(Minecraft.getInstance()));
    }
*///?} else if forge {
/*public final class CinematicTaczClient {
    /^* From the mod's constructor, on the client only. ^/
    public static void init() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Settings.SPEC);
        // the mods list's Config button
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new SettingsScreen(parent)));
        registerScenes();
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) AfterCraft.tick(Minecraft.getInstance());
        });
    }
*///?}

    private static void registerScenes() {
        for (String table : Tacz.TABLES) {
            AddonScenes.register(CinematicTacz.scene(table), (pos, state, player, side) -> new GunSmithScene(pos, state, side));
        }
    }
}
