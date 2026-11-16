package com.example.LiveRoute.cliente;

import com.example.LiveRoute.util.Texto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Busca puntos de interés en OpenStreetMap usando la API de Overpass (gratis, sin API key).
 */
@Component
public class OverpassClient implements ProveedorLugares {

    private static final int MAX_RESULTADOS = 200;

    private final RestClient restClient;
    private final String url;

    @Autowired
    public OverpassClient(@Value("${overpass.url}") String url) {
        this(ClientesHttp.builderConTimeouts(), url);
    }

    /** Para tests: permite pasar un builder conectado a un servidor simulado. */
    OverpassClient(RestClient.Builder builder, String url) {
        this.restClient = ClientesHttp.construir(builder);
        this.url = url;
    }

    @Override
    public List<LugarCercano> buscarLugares(double latitud, double longitud, int radioMetros) {
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("data", consulta(latitud, longitud, radioMetros));

        RespuestaOverpass respuesta;
        try {
            respuesta = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formulario)
                    .retrieve()
                    .body(RespuestaOverpass.class);
        } catch (RestClientException e) {
            throw new ServicioMapasException(e);
        }
        if (respuesta == null || respuesta.elements() == null) {
            return List.of();
        }
        return convertir(respuesta.elements());
    }

    /**
     * Consulta Overpass QL: lugares con nombre de los tipos que nos interesan dentro del radio.
     * "out center" da una coordenada central también para calles/edificios (ways) y relaciones.
     */
    static String consulta(double latitud, double longitud, int radioMetros) {
        String alrededor = "(around:" + radioMetros + "," + latitud + "," + longitud + ")";
        return "[out:json][timeout:15];\n"
                + "(\n"
                + "  nwr" + alrededor + "[\"name\"][\"historic\"];\n"
                + "  nwr" + alrededor + "[\"name\"][\"tourism\"~\"^(attraction|viewpoint|artwork|museum)$\"];\n"
                + "  nwr" + alrededor + "[\"name\"][\"amenity\"~\"^(fountain|place_of_worship)$\"];\n"
                + "  nwr" + alrededor + "[\"name\"][\"leisure\"=\"park\"];\n"
                + ");\n"
                + "out center " + MAX_RESULTADOS + ";";
    }

    private static List<LugarCercano> convertir(List<Elemento> elementos) {
        List<LugarCercano> lugares = new ArrayList<>();
        Set<String> nombresVistos = new HashSet<>();
        for (Elemento e : elementos) {
            Map<String, String> tags = e.tags() == null ? Map.of() : e.tags();
            String nombre = tags.get("name");
            if (nombre == null || nombre.isBlank()) {
                continue;
            }
            TipoLugar tipo = TipoLugar.desdeEtiquetas(tags);
            Double lat = e.lat() != null ? e.lat() : (e.center() != null ? e.center().lat() : null);
            Double lon = e.lon() != null ? e.lon() : (e.center() != null ? e.center().lon() : null);
            if (tipo == null || lat == null || lon == null) {
                continue;
            }
            // El mismo sitio puede venir como punto y como edificio: nos quedamos con uno.
            if (nombresVistos.add(Texto.normalizar(nombre))) {
                lugares.add(new LugarCercano(nombre.strip(), tipo, lat, lon));
            }
        }
        return lugares;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespuestaOverpass(List<Elemento> elements) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Elemento(Double lat, Double lon, Centro center, Map<String, String> tags) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Centro(Double lat, Double lon) {
    }
}
