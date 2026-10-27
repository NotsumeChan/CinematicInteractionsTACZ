package dev.notsume.cinematictacz.mixin;

import dev.dyslexic.cinematic.client.Hands;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Where Cinematic Interactions' hands really are this frame (package-private there): the hammer is drawn in the hand. */
@Mixin(value = Hands.class, remap = false)
public interface HandsAccessor {
    @Invoker("position")
    static Vec3 cinematicTacz$position(boolean right) {
        throw new AssertionError();
    }
}
