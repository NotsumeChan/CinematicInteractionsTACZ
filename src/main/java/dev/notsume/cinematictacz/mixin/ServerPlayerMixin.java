//? if forge {
/*package dev.notsume.cinematictacz.mixin;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraftforge.network.NetworkHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;

/^*
 * Forge only, part of the TACZ bridge (server side). TACZ's table menu reads its table type from the open-menu packet's
 * extra data, which only NetworkHooks.openScreen sends (TACZ opens the table that way itself); its client factory reads
 * it unconditionally. Cinematic Interactions opens a station's menu with a plain openMenu(provider): a table's menu is
 * opened TACZ's way instead, with its type.
 ^/
@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {
    @Inject(method = "openMenu", at = @At("HEAD"), cancellable = true)
    private void cinematicTacz$tableMenu(MenuProvider provider, CallbackInfoReturnable<OptionalInt> cir) {
        if (!(provider instanceof GunSmithTableBlockEntity table)) return;
        ServerPlayer player = (ServerPlayer) (Object) this;
        ResourceLocation type = table.getId() == null ? DefaultAssets.DEFAULT_BLOCK_ID : table.getId();
        NetworkHooks.openScreen(player, table, buf -> buf.writeResourceLocation(type));
        cir.setReturnValue(player.containerMenu instanceof GunSmithTableMenu menu ? OptionalInt.of(menu.containerId) : OptionalInt.empty());
    }
}
*///?}
