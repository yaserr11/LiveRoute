package com.example.LiveRoute.cliente;

/**
 * El servicio de mapas externo ha fallado o no ha respondido a tiempo.
 */
public class ServicioMapasException extends RuntimeException {

    public ServicioMapasException(Throwable causa) {
        super("Servicio de mapas no disponible", causa);
    }
}
