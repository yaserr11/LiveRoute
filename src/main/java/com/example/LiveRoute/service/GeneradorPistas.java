package com.example.LiveRoute.service;

import com.example.LiveRoute.cliente.TipoLugar;
import org.springframework.stereotype.Component;

/**
 * Crea la pista de un reto a partir del tipo de lugar. La pista nunca dice el nombre del lugar,
 * porque el nombre es la respuesta.
 */
@Component
public class GeneradorPistas {

    private static final String PREGUNTA = "¿Cómo se llama?";

    /**
     * @param distanciaMetros distancia desde el punto anterior (o el punto de inicio si es el primer reto)
     * @param direccion       dirección desde el punto anterior: "norte", "sureste"...
     * @param esPrimero       si es el primer reto, se mide desde el punto de inicio
     */
    public String crear(TipoLugar tipo, String dificultad, double distanciaMetros, String direccion, boolean esPrimero) {
        String desde = esPrimero ? "del punto de inicio" : "del reto anterior";
        String distancia = textoDistancia(distanciaMetros);

        String detalle = switch (dificultad) {
            case "facil" -> " Está " + distancia + " al " + direccion + " " + desde + ".";
            case "dificil" -> "";
            default -> " Está " + distancia + " " + desde + ".";
        };
        return plantilla(tipo) + detalle + " " + PREGUNTA;
    }

    private static String plantilla(TipoLugar tipo) {
        return switch (tipo) {
            case MONUMENTO -> "Busca un monumento que recuerda a alguien o algo importante.";
            case SITIO_HISTORICO -> "Busca un lugar con mucha historia.";
            case FUENTE -> "Busca una fuente donde la gente se refresca.";
            case MIRADOR -> "Busca un mirador con buenas vistas.";
            case PARQUE -> "Busca un parque para descansar entre árboles.";
            case IGLESIA -> "Busca una iglesia o templo de la zona.";
            case ATRACCION -> "Busca un lugar que atrae a muchos visitantes.";
            case MUSEO -> "Busca un museo que guarda tesoros del pasado.";
            case OBRA_DE_ARTE -> "Busca una obra de arte al aire libre.";
        };
    }

    /** Redondea a 50 m para que sea una pista y no una medida exacta. */
    private static String textoDistancia(double metros) {
        if (metros < 50) {
            return "a menos de 50 m";
        }
        long redondeado = Math.round(metros / 50) * 50;
        return "a unos " + redondeado + " m";
    }
}
