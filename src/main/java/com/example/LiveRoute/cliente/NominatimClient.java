package com.example.LiveRoute.cliente;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Obtiene el nombre del barrio o ciudad de unas coordenadas usando Nominatim (OpenStreetMap).
 */
@Component
public class NominatimClient implements GeocodificadorInverso {

    private static final Logger log = LoggerFactory.getLogger(NominatimClient.class);

    private final RestClient restClient;
    private final String url;

    @Autowired
    public NominatimClient(@Value("${nominatim.url}") String url) {
        this(ClientesHttp.builderConTimeouts(), url);
    }

    /** Para tests: permite pasar un builder conectado a un servidor simulado. */
    NominatimClient(RestClient.Builder builder, String url) {
        this.restClient = ClientesHttp.construir(builder);
        this.url = url;
    }

    @Override
    public Optional<String> nombreZona(double latitud, double longitud) {
        try {
            RespuestaNominatim respuesta = restClient.get()
                    .uri(url + "?format=jsonv2&zoom=14&lat={lat}&lon={lon}", latitud, longitud)
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "es")
                    .retrieve()
                    .body(RespuestaNominatim.class);
            if (respuesta == null || respuesta.address() == null) {
                return Optional.empty();
            }
            Direccion d = respuesta.address();
            return Stream.of(d.suburb(), d.cityDistrict(), d.town(), d.city(), d.village())
                    .filter(nombre -> nombre != null && !nombre.isBlank())
                    .findFirst();
        } catch (RestClientException e) {
            log.warn("No se pudo obtener el nombre de la zona: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespuestaNominatim(Direccion address) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Direccion(String suburb,
                     @JsonProperty("city_district") String cityDistrict,
                     String town,
                     String city,
                     String village) {
    }
}
