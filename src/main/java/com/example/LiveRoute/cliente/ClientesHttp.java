package com.example.LiveRoute.cliente;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Configuración común de los clientes HTTP externos: timeouts y User-Agent.
 */
final class ClientesHttp {

    static final String USER_AGENT = "LiveRoute/1.0";
    static final Duration TIMEOUT_CONEXION = Duration.ofSeconds(5);
    static final Duration TIMEOUT_LECTURA = Duration.ofSeconds(20);

    private ClientesHttp() {
    }

    /** Builder con timeouts reales, para producción. */
    static RestClient.Builder builderConTimeouts() {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT_CONEXION).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(TIMEOUT_LECTURA);
        return RestClient.builder().requestFactory(factory);
    }

    /** Añade la cabecera User-Agent (los servicios de OpenStreetMap la piden). */
    static RestClient construir(RestClient.Builder builder) {
        return builder.defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT).build();
    }
}
