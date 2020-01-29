package net.dblsaiko.libaddict.mixin;

import net.minecraft.util.Language;
import net.minecraft.util.Util;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.dblsaiko.libaddict.ext.LanguageExt;
import net.dblsaiko.libaddict.util.ParameterizedString;

@Mixin(Language.class)
public class LanguageMixin implements LanguageExt {

    @Shadow @Final private Map<String, String> translations;

    @Shadow private long timeLoaded;

    @Override
    public synchronized void loadExtended(Map<String, ParameterizedString> map) {
        map.forEach((key, value) -> translations.put(key, value.asString()));
        timeLoaded = Util.getMeasuringTimeMs();
    }

}
