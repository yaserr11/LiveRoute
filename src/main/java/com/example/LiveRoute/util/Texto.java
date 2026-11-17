package com.example.LiveRoute.util;

import java.text.Normalizer;
import java.util.Locale;

public final class Texto {

    private Texto() {
    }

    /**
     * Quita tildes, espacios y mayúsculas: "Plaza Málaga" -> "plazamalaga".
     */
    public static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}
