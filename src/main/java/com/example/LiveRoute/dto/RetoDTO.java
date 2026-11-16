package com.example.LiveRoute.dto;

import com.example.LiveRoute.model.Reto;

/**
 * Lo que ve el jugador de un reto: sin la respuesta y sin la gincana completa.
 */
public record RetoDTO(Long id, int orden, String pista, double latitud, double longitud) {

    public static RetoDTO desde(Reto reto) {
        return new RetoDTO(reto.getId(), reto.getOrden(), reto.getPista(), reto.getLatitud(), reto.getLongitud());
    }
}
