package net.dblsaiko.libaddict.ext;

import net.minecraft.util.Language;

import java.util.Map;

import net.dblsaiko.libaddict.util.ParameterizedString;

public interface LanguageExt {

    void loadExtended(Map<String, ParameterizedString> map);

    static LanguageExt from(Language self) {
        return (LanguageExt) self;
    }

}
