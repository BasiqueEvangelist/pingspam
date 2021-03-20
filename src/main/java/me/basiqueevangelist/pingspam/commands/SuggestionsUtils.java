package me.basiqueevangelist.pingspam.commands;

import com.mojang.brigadier.StringReader;

public final class SuggestionsUtils {
    private SuggestionsUtils() {

    }

    public static String wrapString(String wrapped) {
        for (int i = 0; i < wrapped.length(); i++) {
            if (!StringReader.isAllowedInUnquotedString(wrapped.charAt(i))) {
                return '"' + wrapped.replace("\"", "\\\"") + '"';
            }
        }

        return wrapped;
    }
}
