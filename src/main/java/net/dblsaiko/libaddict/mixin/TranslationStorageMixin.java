package net.dblsaiko.libaddict.mixin;

import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.dblsaiko.libaddict.ParameterizedString;
import net.dblsaiko.libaddict.Parser;

@Mixin(TranslationStorage.class)
public class TranslationStorageMixin {

    @Shadow @Final protected Map<String, String> translations;
    private Map<String, ParameterizedString> translations2 = new HashMap<>();

    @Inject(
        method = "load(Lnet/minecraft/resource/ResourceManager;Ljava/util/List;)V",
        at = @At(value = "RETURN")
    )
    private void load(ResourceManager container, List<String> list, CallbackInfo ci) {
        for (String language : list) {
            for (String namespace : container.getAllNamespaces()) {
                Identifier id = new Identifier(namespace, String.format("lang/%s.str", language));
                Map<String, ParameterizedString> newEntries = Parser.include(container, id);
                newEntries.keySet().forEach(k -> translations.remove(k));
                translations2.putAll(newEntries);
            }
        }
    }

    @Inject(
        method = "load(Ljava/io/InputStream;)V",
        at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", shift = Shift.BEFORE),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void load(InputStream inputStream, CallbackInfo ci, JsonElement jsonElement, JsonObject jsonObject, Iterator<Entry<String, JsonElement>> var4, Entry<String, JsonElement> entry) {
        translations2.remove(entry.getKey());
    }

    @Inject(
        method = "translate(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void format(String key, Object[] objects, CallbackInfoReturnable<String> cir) {
        ParameterizedString str = translations2.get(key);
        if (str != null) {
            cir.setReturnValue(str.fmt(objects));
        }
    }

}
