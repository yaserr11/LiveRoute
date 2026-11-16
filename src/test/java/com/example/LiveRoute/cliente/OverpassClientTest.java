package com.example.LiveRoute.cliente;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Prueba OverpassClient contra un servidor HTTP simulado (no se llama a la API real).
 */
class OverpassClientTest {

    private static final String URL = "https://overpass.test/api/interpreter";

    private MockRestServiceServer servidor;
    private OverpassClient cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new OverpassClient(builder, URL);
    }

    @Test
    void conviertePuntosYEdificiosEnLugares() {
        String json = """
                {"elements": [
                  {"type": "node", "id": 1, "lat": 39.667, "lon": -0.217,
                   "tags": {"name": "Fuente de la Plaza", "amenity": "fountain"}},
                  {"type": "way", "id": 2, "center": {"lat": 39.668, "lon": -0.218},
                   "tags": {"name": "Parque Central", "leisure": "park"}},
                  {"type": "node", "id": 3, "lat": 39.669, "lon": -0.219,
                   "tags": {"historic": "monument"}},
                  {"type": "node", "id": 4, "lat": 39.670, "lon": -0.220,
                   "tags": {"name": "Hotel Mar", "tourism": "hotel"}},
                  {"type": "way", "id": 5, "center": {"lat": 39.6671, "lon": -0.2171},
                   "tags": {"name": "fuente de la  plaza", "amenity": "fountain"}},
                  {"type": "relation", "id": 6, "center": {"lat": 39.671, "lon": -0.221},
                   "tags": {"name": "Castillo", "historic": "castle"}}
                ]}""";
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("User-Agent", "LiveRoute/1.0"))
                .andExpect(content().formDataContains(Map.of("data", OverpassClient.consulta(39.6667, -0.2167, 1000))))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<LugarCercano> lugares = cliente.buscarLugares(39.6667, -0.2167, 1000);

        // Sin nombre (3) y tipo que no interesa (4) se descartan; 5 es el mismo nombre que 1.
        assertThat(lugares).containsExactly(
                new LugarCercano("Fuente de la Plaza", TipoLugar.FUENTE, 39.667, -0.217),
                new LugarCercano("Parque Central", TipoLugar.PARQUE, 39.668, -0.218),
                new LugarCercano("Castillo", TipoLugar.SITIO_HISTORICO, 39.671, -0.221));
        servidor.verify();
    }

    @Test
    void laConsultaUsaElCentroYRadioRecibidos() {
        String consulta = OverpassClient.consulta(40.4168, -3.7038, 2500);
        assertThat(consulta).contains("(around:2500,40.4168,-3.7038)").contains("[\"name\"]").contains("out center");
    }

    @Test
    void sinResultadosDevuelveListaVacia() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess("{\"elements\": []}", MediaType.APPLICATION_JSON));
        assertThat(cliente.buscarLugares(39.6, -0.2, 500)).isEmpty();
    }

    @Test
    void errorDelServidorLanzaServicioMapasException() {
        servidor.expect(requestTo(URL)).andRespond(withServerError());
        assertThatThrownBy(() -> cliente.buscarLugares(39.6, -0.2, 500))
                .isInstanceOf(ServicioMapasException.class)
                .hasMessage("Servicio de mapas no disponible");
    }

    @Test
    void timeoutLanzaServicioMapasException() {
        servidor.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("Read timed out")));
        assertThatThrownBy(() -> cliente.buscarLugares(39.6, -0.2, 500))
                .isInstanceOf(ServicioMapasException.class);
    }
}
