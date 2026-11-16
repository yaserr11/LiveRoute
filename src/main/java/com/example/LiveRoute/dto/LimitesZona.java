package com.example.LiveRoute.dto;

/**
 * Límites y mensajes de validación compartidos por "generar" y "previsualizar".
 */
public final class LimitesZona {

    public static final int RADIO_MIN = 200;
    public static final int RADIO_MAX = 3000;
    public static final int RADIO_POR_DEFECTO = 1000;
    public static final int RETOS_MAX = 10;
    public static final int RETOS_POR_DEFECTO = 5;
    public static final String DIFICULTAD_POR_DEFECTO = "media";
    public static final String DIFICULTADES = "facil|media|dificil";

    public static final String MSG_LATITUD_OBLIGATORIA = "La latitud es obligatoria";
    public static final String MSG_LATITUD_RANGO = "La latitud debe estar entre -90 y 90";
    public static final String MSG_LONGITUD_OBLIGATORIA = "La longitud es obligatoria";
    public static final String MSG_LONGITUD_RANGO = "La longitud debe estar entre -180 y 180";
    public static final String MSG_RADIO_MIN = "El radio debe ser de al menos 200 metros";
    public static final String MSG_RADIO_MAX = "El radio no puede superar los 3000 metros";
    public static final String MSG_RETOS_MIN = "Debe haber al menos 1 reto";
    public static final String MSG_RETOS_MAX = "No se pueden generar más de 10 retos";
    public static final String MSG_DIFICULTAD = "La dificultad debe ser facil, media o dificil";

    private LimitesZona() {
    }
}
