//? if fabric {
// Fabric only: Mod Menu is Fabric's; NeoForge has its mods list's Config button.
package dev.notsume.cinematictacz.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Mod Menu's settings button. Mod Menu is optional (suggested); without it the file is edited by hand. */
public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SettingsScreen::new;
    }
}
//?}
