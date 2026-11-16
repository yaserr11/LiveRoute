package com.example.LiveRoute.cliente;

import java.util.Map;

/**
 * Tipos de lugar que usamos para crear retos.
 */
public enum TipoLugar {
    MONUMENTO,
    SITIO_HISTORICO,
    FUENTE,
    MIRADOR,
    PARQUE,
    IGLESIA,
    ATRACCION,
    MUSEO,
    OBRA_DE_ARTE;

    /**
     * Deduce el tipo a partir de las etiquetas de OpenStreetMap.
     * Devuelve null si el lugar no es de ningún tipo que nos interese.
     */
    public static TipoLugar desdeEtiquetas(Map<String, String> tags) {
        String amenity = tags.get("amenity");
        String tourism = tags.get("tourism");
        String historic = tags.get("historic");

        if ("fountain".equals(amenity)) return FUENTE;
        if ("place_of_worship".equals(amenity)) return IGLESIA;
        if ("park".equals(tags.get("leisure"))) return PARQUE;
        if ("viewpoint".equals(tourism)) return MIRADOR;
        if ("museum".equals(tourism)) return MUSEO;
        if ("artwork".equals(tourism)) return OBRA_DE_ARTE;
        if (historic != null) {
            return switch (historic) {
                case "monument", "memorial", "statue" -> MONUMENTO;
                default -> SITIO_HISTORICO;
            };
        }
        if ("attraction".equals(tourism)) return ATRACCION;
        return null;
    }
}
