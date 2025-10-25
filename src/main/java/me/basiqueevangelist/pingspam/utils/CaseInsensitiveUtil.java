package me.basiqueevangelist.pingspam.utils;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class CaseInsensitiveUtil {
    private CaseInsensitiveUtil() {

    }

    private static Hash.Strategy<String> strategyIgnoringCase() {
        return new Hash.Strategy<>() {
            @Override
            public int hashCode(String o) {
                int h = 0;
                for (int i = 0; i < o.length(); i++) {
                    h = 31 * h + Character.toLowerCase(o.charAt(i));
                }
                return h;
            }

            @Override
            public boolean equals(String a, String b) {
                return a.equalsIgnoreCase(b);
            }
        };
    }

    public static Set<String> setIgnoringCase() {
        return new ObjectOpenCustomHashSet<>(strategyIgnoringCase());
    }

    public static Set<String> treeSetIgnoringCase() {
        return new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    }

    public static <V> Map<String, V> mapIgnoringCase() {
        return new Object2ObjectOpenCustomHashMap<>(strategyIgnoringCase());
    }
}
