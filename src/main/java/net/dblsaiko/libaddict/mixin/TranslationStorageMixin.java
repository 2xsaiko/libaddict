package net.dblsaiko.libaddict.mixin;

import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.dblsaiko.libaddict.ext.TranslationStorageExt;
import net.dblsaiko.libaddict.parser.Parser;
import net.dblsaiko.libaddict.util.ParameterizedString;

@Mixin(TranslationStorage.class)
public class TranslationStorageMixin implements TranslationStorageExt {

    @Shadow
    @Final
    @Mutable
    private Map<String, String> translations;
    private final Map<String, ParameterizedString> translations2 = new HashMap<>();

    public Map<String, ParameterizedString> getExtendedTranslations() {
        return translations2;
    }

    @Inject(
        method = "load(Lnet/minecraft/resource/ResourceManager;Ljava/util/List;)Lnet/minecraft/client/resource/language/TranslationStorage;",
        at = @At(value = "RETURN"),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void load(ResourceManager resourceManager, List<LanguageDefinition> list, CallbackInfoReturnable<TranslationStorage> cir) {
        TranslationStorageMixin rv = (TranslationStorageMixin) (Object) cir.getReturnValue();
        Map<String, String> translations = new HashMap<>(rv.translations); // this is immutable
        Map<String, ParameterizedString> translations2 = rv.translations2;
        for (LanguageDefinition language : list) {
            for (String namespace : resourceManager.getAllNamespaces()) {
                Identifier id = new Identifier(namespace, String.format("lang/%s.str", language.getCode()));
                Map<String, ParameterizedString> newEntries = Parser.include(resourceManager, id);
                newEntries.forEach((k, v) -> translations.put(k, v.asString()));
                translations2.putAll(newEntries);
            }
        }
        rv.translations = Collections.unmodifiableMap(translations);
    }

}
