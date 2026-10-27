package dev.notsume.cinematictacz.tacz;

import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.network.message.ClientMessageCraft;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** The client side of {@link Tacz}: TACZ's craft packet and table window. Keep it out of common code. */
public final class TaczClient {
    private TaczClient() {}

    /** Asks the server to craft {@code recipe} in the open table menu {@code menuId}, as TACZ's own Craft button does. */
    public static void craft(ResourceLocation recipe, int menuId) {
        ClientPlayNetworking.send(new ClientMessageCraft(recipe, menuId));
    }

    public static boolean isTableWindow(@Nullable Screen screen) {
        return screen instanceof GunSmithTableScreen;
    }
}
