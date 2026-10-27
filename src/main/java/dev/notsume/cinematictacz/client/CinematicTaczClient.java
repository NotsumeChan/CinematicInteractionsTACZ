package dev.notsume.cinematictacz.client;

import dev.dyslexic.cinematic.api.client.AddonScenes;
import dev.notsume.cinematictacz.CinematicTacz;
import net.fabricmc.api.ClientModInitializer;

public final class CinematicTaczClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        for (String table : CinematicTacz.TABLES.keySet()) {
            AddonScenes.register(CinematicTacz.scene(table), (pos, state, player, side) -> new GunSmithScene(pos, state, side));
        }
    }
}
