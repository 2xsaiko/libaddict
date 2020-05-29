package net.dblsaiko.libaddict.mixin;

import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.util.Language;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.dblsaiko.libaddict.ext.TranslationStorageExt;
import net.dblsaiko.libaddict.util.ParameterizedString;

@Mixin(I18n.class)
public class I18nMixin {

    @Shadow private static volatile Language field_25290;

    @Inject(
        method = "translate(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void translate(String key, Object[] args, CallbackInfoReturnable<String> cir) {
        if (field_25290 instanceof TranslationStorage) {
            ParameterizedString str = TranslationStorageExt.from((TranslationStorage) field_25290).getExtendedTranslations().get(key);
            if (str != null) {
                cir.setReturnValue(str.fmt(args));
            }
        }
    }

}
