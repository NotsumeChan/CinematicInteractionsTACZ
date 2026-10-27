package dev.notsume.cinematictacz.client;

import dev.dyslexic.cinematic.api.client.AddonScenes;
import dev.notsume.cinematictacz.CinematicTacz;
import dev.notsume.cinematictacz.tacz.Tacz;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class CinematicTaczClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        for (String table : Tacz.TABLES) {
            AddonScenes.register(CinematicTacz.scene(table), (pos, state, player, side) -> new GunSmithScene(pos, state, side));
        }
        ClientTickEvents.END_CLIENT_TICK.register(AfterCraft::tick);
    }
}
