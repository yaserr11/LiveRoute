package com.example.LiveRoute.cliente;

import java.util.List;

/**
 * Busca lugares con nombre alrededor de un punto. Hoy lo implementa OverpassClient;
 * se puede cambiar por otro proveedor sin tocar el servicio.
 */
public interface ProveedorLugares {

    /**
     * @throws ServicioMapasException si el servicio de mapas falla o tarda demasiado
     */
    List<LugarCercano> buscarLugares(double latitud, double longitud, int radioMetros);
}
