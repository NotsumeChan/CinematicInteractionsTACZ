package dev.notsume.cinematictacz.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.client.renderer.block.GunSmithTableRenderer;
import dev.notsume.cinematictacz.client.CraftFx;
import dev.notsume.cinematictacz.tacz.TaczClient;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Part of the TACZ bridge: the ammo bench's press strokes down while the scene crafts on it. Optional (require 0): with a
 * TACZ whose table renderer differs, the press just stays still.
 */
@Mixin(GunSmithTableRenderer.class)
abstract class TaczTableRendererMixin {
    // Loom does not remap this target (another mod's method): the descriptor with dev names and the one a real game sees
    // (intermediary names; TaCZ Refabricated's jar keeps the method's own name, render) are both listed, and whichever
    // exists is hooked.
    private static final String RENDER = "render(Lcom/tacz/guns/block/entity/GunSmithTableBlockEntity;"
            + "FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V";
    private static final String RENDER_INTERMEDIARY = "render(Lcom/tacz/guns/block/entity/GunSmithTableBlockEntity;"
            + "FLnet/minecraft/class_4587;Lnet/minecraft/class_4597;II)V";

    @Inject(method = {RENDER, RENDER_INTERMEDIARY}, at = @At("HEAD"), require = 0)
    private void cinematicTacz$pressDown(GunSmithTableBlockEntity table, float partialTick, PoseStack pose,
                                         MultiBufferSource buffers, int light, int overlay, CallbackInfo ci) {
        float stroke = CraftFx.pressStroke(table.getBlockPos());
        if (stroke != 0) TaczClient.press(table, stroke);
    }

    @Inject(method = {RENDER, RENDER_INTERMEDIARY}, at = @At("TAIL"), require = 0)
    private void cinematicTacz$pressBack(GunSmithTableBlockEntity table, float partialTick, PoseStack pose,
                                         MultiBufferSource buffers, int light, int overlay, CallbackInfo ci) {
        if (CraftFx.pressStroke(table.getBlockPos()) != 0) TaczClient.press(table, 0);
    }
}
