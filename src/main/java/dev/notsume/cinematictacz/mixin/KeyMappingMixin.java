package dev.notsume.cinematictacz.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.notsume.cinematictacz.tacz.Tacz;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TACZ binds shoot and aim to the mouse buttons that also attack and use. Forge lets one button press several key
 * mappings; vanilla, and so Fabric, gives each button to one mapping only, and TaCZ Refabricated leaves either the gun
 * or the hand deaf. A press now reaches every mapping on that button when one of them is TACZ's, as on Forge; TACZ
 * itself keeps the hand from attacking while a gun is held.
 */
@Mixin(KeyMapping.class)
abstract class KeyMappingMixin {
    @Shadow
    @Final
    private static Map<String, KeyMapping> ALL;
    @Shadow
    @Final
    private static Map<InputConstants.Key, KeyMapping> MAP;
    @Shadow
    private int clickCount;

    @Inject(method = "click", at = @At("TAIL"))
    private static void cinematicTacz$clickShared(InputConstants.Key key, CallbackInfo ci) {
        for (KeyMapping other : cinematicTacz$sharing(key)) {
            ((KeyMappingMixin) (Object) other).clickCount++;
        }
    }

    @Inject(method = "set", at = @At("TAIL"))
    private static void cinematicTacz$setShared(InputConstants.Key key, boolean down, CallbackInfo ci) {
        for (KeyMapping other : cinematicTacz$sharing(key)) {
            other.setDown(down);
        }
    }

    /** The mappings on {@code key} besides the one vanilla gave it to, when TACZ is on either side. */
    @Unique
    private static List<KeyMapping> cinematicTacz$sharing(InputConstants.Key key) {
        KeyMapping owner = MAP.get(key);
        if (owner == null) return List.of();
        boolean taczOwns = Tacz.KEY_CATEGORY.equals(owner.getCategory());
        List<KeyMapping> others = new ArrayList<>(1);
        for (KeyMapping mapping : ALL.values()) {
            if (mapping != owner && (taczOwns || Tacz.KEY_CATEGORY.equals(mapping.getCategory()))
                    && key.equals(KeyBindingHelper.getBoundKeyOf(mapping))) {
                others.add(mapping);
            }
        }
        return others;
    }
}
