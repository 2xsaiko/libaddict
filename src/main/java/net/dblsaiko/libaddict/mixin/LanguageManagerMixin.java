package net.dblsaiko.libaddict.mixin;

import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Language;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dblsaiko.libaddict.ext.LanguageExt;
import net.dblsaiko.libaddict.ext.TranslationStorageExt;

@Mixin(LanguageManager.class)
public class LanguageManagerMixin {

    @Shadow @Final protected static TranslationStorage STORAGE;

    @Inject(method = "apply(Lnet/minecraft/resource/ResourceManager;)V", at = @At("RETURN"))
    private void apply(ResourceManager manager, CallbackInfo ci) {
        LanguageExt.from(Language.getInstance()).loadExtended(TranslationStorageExt.from(STORAGE).getExtendedTranslations());
    }

}
