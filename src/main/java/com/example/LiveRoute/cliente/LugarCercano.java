package com.example.LiveRoute.cliente;

/**
 * Un lugar real con nombre encontrado cerca del punto elegido.
 */
public record LugarCercano(String nombre, TipoLugar tipo, double latitud, double longitud) {
}
