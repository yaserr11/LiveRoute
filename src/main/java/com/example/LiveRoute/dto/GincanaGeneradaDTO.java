package com.example.LiveRoute.dto;

import java.util.List;

/**
 * Gincana recién generada con sus retos. Los retos van como RetoDTO, así que nunca incluyen la respuesta.
 */
public record GincanaGeneradaDTO(Long id,
                                 String titulo,
                                 String lugar,
                                 double latitud,
                                 double longitud,
                                 String dificultad,
                                 List<RetoDTO> retos) {
}
