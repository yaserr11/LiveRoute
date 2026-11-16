package com.example.LiveRoute.cliente;

import java.util.Optional;

/**
 * Da un nombre legible (barrio, ciudad...) a unas coordenadas.
 */
public interface GeocodificadorInverso {

    /**
     * Nunca lanza excepción: si el servicio falla devuelve Optional.empty().
     */
    Optional<String> nombreZona(double latitud, double longitud);
}
