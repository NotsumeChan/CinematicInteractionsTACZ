package dev.notsume.cinematictacz.mixin;

import com.tacz.guns.client.gui.GunSmithTableScreen;
import dev.dyslexic.cinematic.client.Scene;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    /**
     * TACZ's table window only ever stands behind the scene, which hides it. Opened any other way (out of reach for the
     * scene, sneaking, TACZ's interact key) the menu is closed again and nothing shows but a word to come closer.
     */
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void cinematicTacz$sceneOnly(@Nullable Screen next, CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        if (next instanceof GunSmithTableScreen && !Scene.active() && mc.player != null) {
            ci.cancel();
            mc.player.closeContainer();
            mc.player.displayClientMessage(Component.translatable("cinematic_tacz.closer"), true);
        }
    }
}
