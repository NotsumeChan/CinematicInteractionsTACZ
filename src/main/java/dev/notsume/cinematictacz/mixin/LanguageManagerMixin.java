package dev.notsume.cinematictacz.mixin;

import net.minecraft.client.resources.language.LanguageManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;
import java.util.List;

@Mixin(LanguageManager.class)
abstract class LanguageManagerMixin {
    /**
     * TACZ, its gunpacks and this add-on are translated to es_es only; Minecraft goes from es_cl, es_mx ... straight to
     * en_us. Any other Spanish loads es_es in between, so what it lacks reads in Spanish rather than English.
     */
    @ModifyArg(method = "onResourceManagerReload", index = 1, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/language/ClientLanguage;loadFrom(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;Z)Lnet/minecraft/client/resources/language/ClientLanguage;"))
    private List<String> cinematicTacz$spanishFallback(List<String> languages) {
        String chosen = languages.get(languages.size() - 1);
        if (!chosen.startsWith("es_") || chosen.equals("es_es")) return languages;
        List<String> withSpain = new ArrayList<>(languages);
        withSpain.add(withSpain.size() - 1, "es_es");
        return withSpain;
    }
}
