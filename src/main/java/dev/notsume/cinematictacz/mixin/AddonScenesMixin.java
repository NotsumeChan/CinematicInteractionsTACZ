package dev.notsume.cinematictacz.mixin;

import com.tacz.guns.block.AbstractGunSmithTableBlock;
import dev.dyslexic.cinematic.api.client.AddonScenes;
import dev.notsume.cinematictacz.client.Settings;
import dev.notsume.cinematictacz.tacz.Tacz;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A table whose scene the player turned off has no scene: Cinematic Interactions then opens the block's own window, as
 * for one of its own stations turned off. Its add-on API has no such switch per scene (asked of its owner, see the
 * roadmap).
 */
@Mixin(value = AddonScenes.class, remap = false)
abstract class AddonScenesMixin {
    // available is Cinematic Interactions' own method (not remapped); its parameter is, so it is matched by name only
    @Inject(method = "available", at = @At("HEAD"), cancellable = true)
    private static void cinematicTacz$sceneOff(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof AbstractGunSmithTableBlock && !Settings.scene(Tacz.shape(state))) {
            cir.setReturnValue(false);
        }
    }
}
