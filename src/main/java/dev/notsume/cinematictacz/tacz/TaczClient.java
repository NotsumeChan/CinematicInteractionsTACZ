package dev.notsume.cinematictacz.tacz;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.network.message.ClientMessageCraft;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** The client side of {@link Tacz}: TACZ's craft packet, table window, table models and gun animations. */
public final class TaczClient {
    private TaczClient() {}

    // ponytail: the default gunpack's bone name; a gunpack whose ammo bench has no "loader" just gets no press stroke
    /** The reloading press of the ammo bench's model. The whole press is this one bone, its base included. */
    private static final String PRESS_BONE = "loader";

    /** Asks the server to craft {@code recipe} in the open table menu {@code menuId}, as TACZ's own Craft button does. */
    public static void craft(ResourceLocation recipe, int menuId) {
        ClientPlayNetworking.send(new ClientMessageCraft(recipe, menuId));
    }

    public static boolean isTableWindow(@Nullable Screen screen) {
        return screen instanceof GunSmithTableScreen;
    }

    /** Plays the inspect animation of the gun in the player's main hand, as TACZ's inspect key does. */
    public static void inspect(LocalPlayer player) {
        IClientPlayerGunOperator.fromLocalPlayer(player).inspect();
    }

    /**
     * Sets how far down (blocks) the press of {@code table}'s model is pushed for the next draw. The model is shared by
     * every table of its type, so a stroke is set just before one table is drawn and undone right after.
     */
    public static void press(GunSmithTableBlockEntity table, float depth) {
        ResourceLocation type = table.getId() == null ? DefaultAssets.DEFAULT_BLOCK_ID : table.getId();
        TimelessAPI.getClientBlockIndex(type).ifPresent(index -> {
            BedrockPart press = index.getModel() == null ? null : index.getModel().getNode(PRESS_BONE);
            // the model is drawn upside down (TACZ turns it 180 degrees round Z): its +y points down in the world
            if (press != null) press.offsetY = depth;
        });
    }
}
