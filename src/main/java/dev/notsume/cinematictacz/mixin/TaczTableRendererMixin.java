package dev.notsume.cinematictacz.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.client.renderer.block.GunSmithTableRenderer;
import dev.notsume.cinematictacz.client.CraftFx;
import dev.notsume.cinematictacz.tacz.TaczClient;
//? if >=26.1 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
*///?} else {
import net.minecraft.client.renderer.MultiBufferSource;
//?}
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Part of the TACZ bridge: the ammo bench's press strokes down, the attachment bench's build plate dips and the forge's
 * tool leaves the gun smith table for the hand while the scene crafts on them. Optional (require 0): with a
 * TACZ whose table renderer differs, the press just stays still.
 */
@Mixin(GunSmithTableRenderer.class)
abstract class TaczTableRendererMixin {
    //? if >=26.1 {
    /*// 26.1: drawn from a render state; the model's geometry is captured at once in submit, so the bones are set around it
    private static final String SUBMIT = "submit(Lcom/tacz/guns/client/renderer/block/GunSmithTableRenderer$GunSmithTableRenderState;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;"
            + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V";

    @Inject(method = SUBMIT, at = @At("HEAD"), require = 0)
    private void cinematicTacz$pressDown(GunSmithTableRenderer.GunSmithTableRenderState state, PoseStack pose,
                                         SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        if (Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.getBlockEntity(state.blockPos) instanceof GunSmithTableBlockEntity table) {
            cinematicTacz$set(table);
        }
    }

    @Inject(method = SUBMIT, at = @At("TAIL"), require = 0)
    private void cinematicTacz$pressBack(GunSmithTableRenderer.GunSmithTableRenderState state, PoseStack pose,
                                         SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        if (Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.getBlockEntity(state.blockPos) instanceof GunSmithTableBlockEntity table) {
            cinematicTacz$reset(table);
        }
    }
    *///?} else {
    // Not remapped (another mod's method): the descriptor with dev names and the one a real Fabric game sees
    // (intermediary names; TaCZ Refabricated's jar keeps the method's own name, render) are both listed, and whichever
    // exists is hooked. Forge's TACZ, in SRG names, also keeps render (only its bridge is m_6922_), with Mojang's class
    // names: the dev descriptor.
    private static final String RENDER = "render(Lcom/tacz/guns/block/entity/GunSmithTableBlockEntity;"
            + "FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V";
    private static final String RENDER_INTERMEDIARY = "render(Lcom/tacz/guns/block/entity/GunSmithTableBlockEntity;"
            + "FLnet/minecraft/class_4587;Lnet/minecraft/class_4597;II)V";

    @Inject(method = {RENDER, RENDER_INTERMEDIARY}, at = @At("HEAD"), require = 0, remap = false)
    private void cinematicTacz$pressDown(GunSmithTableBlockEntity table, float partialTick, PoseStack pose,
                                         MultiBufferSource buffers, int light, int overlay, CallbackInfo ci) {
        cinematicTacz$set(table);
    }

    @Inject(method = {RENDER, RENDER_INTERMEDIARY}, at = @At("TAIL"), require = 0, remap = false)
    private void cinematicTacz$pressBack(GunSmithTableBlockEntity table, float partialTick, PoseStack pose,
                                         MultiBufferSource buffers, int light, int overlay, CallbackInfo ci) {
        cinematicTacz$reset(table);
    }
    //?}

    /** Before the table is drawn: its press, build plate and tools as the scene has them now. */
    @Unique
    private static void cinematicTacz$set(GunSmithTableBlockEntity table) {
        float stroke = CraftFx.pressStroke(table.getBlockPos()), plate = CraftFx.plateDrop(table.getBlockPos());
        if (stroke != 0) TaczClient.press(table, stroke);
        if (plate >= 0) TaczClient.print(table, plate, true);
        TaczClient.Tool held = CraftFx.held(table.getBlockPos());
        if (held != null) TaczClient.hide(table, held, true);
    }

    /** After: the shared model back as it was, for the next table of its type. */
    @Unique
    private static void cinematicTacz$reset(GunSmithTableBlockEntity table) {
        if (CraftFx.pressStroke(table.getBlockPos()) != 0) TaczClient.press(table, 0);
        if (CraftFx.plateDrop(table.getBlockPos()) >= 0) TaczClient.print(table, 0, false);
        for (TaczClient.Tool tool : TaczClient.Tool.values()) TaczClient.hide(table, tool, false);
    }
}
