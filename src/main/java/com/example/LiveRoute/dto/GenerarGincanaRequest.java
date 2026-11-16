package com.example.LiveRoute.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import static com.example.LiveRoute.dto.LimitesZona.*;

/**
 * Lo que envía la app para generar una gincana. El centro y el radio los elige el jugador.
 * Los campos opcionales que lleguen vacíos toman su valor por defecto en el servicio.
 */
public record GenerarGincanaRequest(
        @NotNull(message = MSG_LATITUD_OBLIGATORIA)
        @DecimalMin(value = "-90", message = MSG_LATITUD_RANGO)
        @DecimalMax(value = "90", message = MSG_LATITUD_RANGO)
        Double latitud,

        @NotNull(message = MSG_LONGITUD_OBLIGATORIA)
        @DecimalMin(value = "-180", message = MSG_LONGITUD_RANGO)
        @DecimalMax(value = "180", message = MSG_LONGITUD_RANGO)
        Double longitud,

        @Min(value = RADIO_MIN, message = MSG_RADIO_MIN)
        @Max(value = RADIO_MAX, message = MSG_RADIO_MAX)
        Integer radioMetros,

        @Min(value = 1, message = MSG_RETOS_MIN)
        @Max(value = RETOS_MAX, message = MSG_RETOS_MAX)
        Integer numeroRetos,

        @Pattern(regexp = DIFICULTADES, message = MSG_DIFICULTAD)
        String dificultad
) {
}
