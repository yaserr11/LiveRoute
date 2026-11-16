package com.example.LiveRoute.cliente;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Prueba NominatimClient contra un servidor HTTP simulado (no se llama a la API real).
 */
class NominatimClientTest {

    private static final String URL = "https://nominatim.test/reverse";

    private MockRestServiceServer servidor;
    private NominatimClient cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new NominatimClient(builder, URL);
    }

    @Test
    void prefiereElBarrio() {
        servidor.expect(requestTo(startsWith(URL)))
                .andExpect(queryParam("lat", "39.6667"))
                .andExpect(queryParam("lon", "-0.2167"))
                .andExpect(header("User-Agent", "LiveRoute/1.0"))
                .andRespond(withSuccess("""
                        {"address": {"suburb": "Puerto de Sagunto", "city": "Sagunto", "country": "España"}}""",
                        MediaType.APPLICATION_JSON));

        assertThat(cliente.nombreZona(39.6667, -0.2167)).contains("Puerto de Sagunto");
        servidor.verify();
    }

    @Test
    void sinBarrioUsaLaCiudad() {
        servidor.expect(requestTo(startsWith(URL)))
                .andRespond(withSuccess("{\"address\": {\"city\": \"Valencia\"}}", MediaType.APPLICATION_JSON));
        assertThat(cliente.nombreZona(39.47, -0.37)).contains("Valencia");
    }

    @Test
    void siFallaDevuelveVacioSinLanzar() {
        servidor.expect(requestTo(startsWith(URL))).andRespond(withServerError());
        assertThat(cliente.nombreZona(39.47, -0.37)).isEmpty();
    }

    @Test
    void timeoutDevuelveVacio() {
        servidor.expect(requestTo(startsWith(URL))).andRespond(withException(new SocketTimeoutException()));
        assertThat(cliente.nombreZona(39.47, -0.37)).isEmpty();
    }
}
