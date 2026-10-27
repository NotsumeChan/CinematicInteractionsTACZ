//? if neoforge {
/*package dev.notsume.cinematictacz.mixin;

import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.client.extensions.IMenuProviderExtension;
import org.spongepowered.asm.mixin.Mixin;

/^*
 * NeoForge only, part of the TACZ bridge (both sides). The menu of TACZ's NeoForge port learns its table type from the
 * extra data of the open-menu packet, which TACZ writes when it opens the table itself. Cinematic Interactions opens a
 * station's menu with a plain openMenu(provider), so the client's menu had no table type and the scene no recipes.
 * NeoForge asks every menu provider for that data (writeClientSideData, on every openMenu): the table writes its type.
 * When TACZ opens it, its own copy follows and is never read.
 ^/
@Mixin(value = GunSmithTableBlockEntity.class, remap = false)
abstract class GunSmithTableMenuDataMixin implements IMenuProviderExtension {
    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        if (menu instanceof GunSmithTableMenu table && table.getBlockId() != null) {
            buffer.writeResourceLocation(table.getBlockId());
        }
    }
}
*///?}
