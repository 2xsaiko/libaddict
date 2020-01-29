package net.dblsaiko.libaddict.ext;

import net.minecraft.client.resource.language.TranslationStorage;

import java.util.Map;

import net.dblsaiko.libaddict.util.ParameterizedString;

public interface TranslationStorageExt {

    Map<String, ParameterizedString> getExtendedTranslations();

    static TranslationStorageExt from(TranslationStorage self) {
        return (TranslationStorageExt) self;
    }

}
