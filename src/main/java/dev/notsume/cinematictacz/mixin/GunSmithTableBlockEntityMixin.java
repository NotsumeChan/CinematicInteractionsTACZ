package dev.notsume.cinematictacz.mixin;

import com.tacz.guns.block.AbstractGunSmithTableBlock;
import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Part of the TACZ bridge. Every block of a two-block table gets a block entity, but only the root's is given the table
 * type when placed: the other one's is empty and reads as the default gun smith table. TACZ's own use opens the root's
 * menu; Cinematic Interactions opens the clicked block's, so clicking the upper half of an attachment bench opened a
 * gun smith table menu and the scene found no recipes. The other block now answers with its root's type.
 */
@Mixin(value = GunSmithTableBlockEntity.class, remap = false)
abstract class GunSmithTableBlockEntityMixin {
    // getId is TACZ's own method (not remapped); its return type is, so it is matched by name only
    @Inject(method = "getId", at = @At("HEAD"), cancellable = true)
    private void cinematicTacz$rootType(CallbackInfoReturnable<ResourceLocation> cir) {
        BlockEntity self = (BlockEntity) (Object) this;
        Level level = self.getLevel();
        BlockState state = self.getBlockState();
        if (level == null || !(state.getBlock() instanceof AbstractGunSmithTableBlock table) || table.isRoot(state)) return;
        if (level.getBlockEntity(table.getRootPos(self.getBlockPos(), state)) instanceof GunSmithTableBlockEntity root && root != self) {
            cir.setReturnValue(root.getId());
        }
    }
}
