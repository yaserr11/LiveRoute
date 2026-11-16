package com.example.LiveRoute.cliente;

import com.example.LiveRoute.util.Texto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Busca puntos de interés en OpenStreetMap usando la API de Overpass (gratis, sin API key).
 * Prueba los servidores de "overpass.urls" en orden: si uno falla o tarda demasiado, pasa al
 * siguiente. Solo lanza ServicioMapasException si fallan todos.
 * <p>
 * Un servidor que acaba de fallar queda "en pausa" unos minutos: se prueba el último, para que
 * las siguientes peticiones no esperen otra vez su timeout. Se sigue usando como último recurso.
 */
@Component
public class OverpassClient implements ProveedorLugares {

    private static final Logger log = LoggerFactory.getLogger(OverpassClient.class);
    private static final int MAX_RESULTADOS = 200;
    static final Duration PAUSA_TRAS_FALLO = Duration.ofMinutes(2);

    private final RestClient restClient;
    private final List<String> urls;
    private final Clock reloj;
    private final Map<String, Instant> enPausaHasta = new ConcurrentHashMap<>();

    @Autowired
    public OverpassClient(@Value("${overpass.urls}") List<String> urls) {
        this(ClientesHttp.builderConTimeouts(), urls, Clock.systemUTC());
    }

    /** Para tests: permite pasar un builder conectado a un servidor simulado y un reloj fijo. */
    OverpassClient(RestClient.Builder builder, List<String> urls, Clock reloj) {
        List<String> limpias = urls == null ? List.of()
                : urls.stream().map(String::strip).filter(u -> !u.isEmpty()).toList();
        if (limpias.isEmpty()) {
            throw new IllegalArgumentException("overpass.urls debe tener al menos un servidor");
        }
        this.restClient = ClientesHttp.construir(builder);
        this.urls = limpias;
        this.reloj = reloj;
    }

    /** Orden configurado, pero con los servidores en pausa al final. */
    List<String> ordenDeServidores() {
        Instant ahora = reloj.instant();
        List<String> disponibles = new ArrayList<>();
        List<String> enPausa = new ArrayList<>();
        for (String url : urls) {
            Instant hasta = enPausaHasta.get(url);
            (hasta != null && ahora.isBefore(hasta) ? enPausa : disponibles).add(url);
        }
        disponibles.addAll(enPausa);
        return disponibles;
    }

    @Override
    public List<LugarCercano> buscarLugares(double latitud, double longitud, int radioMetros) {
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("data", consulta(latitud, longitud, radioMetros));

        RestClientException ultimoFallo = null;
        List<RestClientException> fallos = new ArrayList<>();
        List<String> orden = ordenDeServidores();
        for (String url : orden) {
            long inicio = System.nanoTime();
            try {
                List<LugarCercano> lugares = consultarServidor(url, formulario);
                enPausaHasta.remove(url);
                log.info("Overpass respondió desde {} en {} ms ({} lugares)",
                        url, (System.nanoTime() - inicio) / 1_000_000, lugares.size());
                return lugares;
            } catch (RestClientException e) {
                enPausaHasta.put(url, reloj.instant().plus(PAUSA_TRAS_FALLO));
                log.warn("Overpass falló en {} tras {} ms: {}",
                        url, (System.nanoTime() - inicio) / 1_000_000, primeraLinea(e.getMessage()));
                fallos.add(e);
                ultimoFallo = e;
            }
        }
        ServicioMapasException error = new ServicioMapasException(ultimoFallo);
        fallos.stream().filter(f -> f != error.getCause()).forEach(error::addSuppressed);
        log.error("Fallaron los {} servidores de Overpass", orden.size());
        throw error;
    }

    private List<LugarCercano> consultarServidor(String url, MultiValueMap<String, String> formulario) {
        RespuestaOverpass respuesta = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formulario)
                .retrieve()
                .body(RespuestaOverpass.class);
        if (respuesta == null) {
            return List.of();
        }
        // Overpass a veces responde 200 con un resultado a medias y un aviso
        // ("runtime error: Query timed out..."). Eso cuenta como fallo: probamos otro servidor.
        if (respuesta.remark() != null && respuesta.remark().contains("error")) {
            throw new RespuestaIncompletaException(respuesta.remark());
        }
        return respuesta.elements() == null ? List.of() : convertir(respuesta.elements());
    }

    /** Los errores de Overpass traen páginas de texto largas; en el log basta la primera línea. */
    private static String primeraLinea(String mensaje) {
        if (mensaje == null) {
            return "";
        }
        // Spring escribe los saltos de línea del cuerpo de error como "<EOL>".
        String linea = mensaje.split("<EOL>|\\R", 2)[0];
        return linea.length() > 200 ? linea.substring(0, 200) + "..." : linea;
    }

    /** El servidor respondió, pero con un resultado incompleto. */
    static class RespuestaIncompletaException extends RestClientException {
        RespuestaIncompletaException(String aviso) {
            super("Respuesta incompleta de Overpass: " + aviso);
        }
    }

    /**
     * Consulta Overpass QL: lugares con nombre de los tipos que nos interesan dentro del radio.
     * "out center" da una coordenada central también para calles/edificios (ways) y relaciones.
     */
    static String consulta(double latitud, double longitud, int radioMetros) {
        String alrededor = "(around:" + radioMetros + "," + latitud + "," + longitud + ")";
        return "[out:json][timeout:25];\n"
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
    record RespuestaOverpass(List<Elemento> elements, String remark) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Elemento(Double lat, Double lon, Centro center, Map<String, String> tags) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Centro(Double lat, Double lon) {
    }
}
