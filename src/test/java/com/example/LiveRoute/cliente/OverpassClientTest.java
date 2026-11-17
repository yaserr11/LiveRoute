package com.example.LiveRoute.cliente;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Prueba OverpassClient contra servidores HTTP simulados (no se llama a la API real).
 */
@ExtendWith(OutputCaptureExtension.class)
class OverpassClientTest {

    private static final String PRINCIPAL = "https://principal.test/api/interpreter";
    private static final String ESPEJO1 = "https://espejo1.test/api/interpreter";
    private static final String ESPEJO2 = "https://espejo2.test/api/interpreter";

    private static final String UN_LUGAR = """
            {"elements": [{"type": "node", "id": 1, "lat": 39.667, "lon": -0.217,
                           "tags": {"name": "Fuente de la Plaza", "amenity": "fountain"}}]}""";
    private static final String DOS_LUGARES = """
            {"elements": [
              {"type": "node", "id": 1, "lat": 39.667, "lon": -0.217,
               "tags": {"name": "Fuente de la Plaza", "amenity": "fountain"}},
              {"type": "node", "id": 2, "lat": 39.668, "lon": -0.218,
               "tags": {"name": "Parque Central", "leisure": "park"}}]}""";

    private MockRestServiceServer servidor;
    private OverpassClient cliente;
    private RelojMovible reloj;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        reloj = new RelojMovible(Instant.parse("2026-11-16T12:00:00Z"));
        cliente = new OverpassClient(builder, List.of(PRINCIPAL, ESPEJO1, ESPEJO2), reloj);
    }

    // ---------- Conversión de la respuesta ----------

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
        servidor.expect(requestTo(PRINCIPAL))
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
        servidor.expect(requestTo(PRINCIPAL)).andRespond(withSuccess("{\"elements\": []}", MediaType.APPLICATION_JSON));
        assertThat(cliente.buscarLugares(39.6, -0.2, 500)).isEmpty();
        servidor.verify();
    }

    // ---------- Varios servidores ----------

    @Test
    void siElPrincipalRespondeNoSePruebanLosEspejos(CapturedOutput salida) {
        servidor.expect(requestTo(PRINCIPAL)).andRespond(withSuccess(UN_LUGAR, MediaType.APPLICATION_JSON));
        servidor.expect(never(), requestTo(ESPEJO1));

        assertThat(cliente.buscarLugares(39.6, -0.2, 500)).hasSize(1);
        servidor.verify();
        assertThat(salida).contains("Overpass respondió desde " + PRINCIPAL);
    }

    @Test
    void siElPrincipalFallaUsaElSiguiente(CapturedOutput salida) {
        servidor.expect(requestTo(PRINCIPAL)).andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                .body("# 502 Gateway Timeout\n\nThis Overpass API server is currently at capacity."));
        servidor.expect(requestTo(ESPEJO1)).andRespond(withSuccess(DOS_LUGARES, MediaType.APPLICATION_JSON));

        assertThat(cliente.buscarLugares(39.6, -0.2, 500)).hasSize(2);
        servidor.verify();
        assertThat(salida)
                .contains("Overpass falló en " + PRINCIPAL)
                .contains("Overpass respondió desde " + ESPEJO1)
                .doesNotContain("currently at capacity"); // solo la primera línea del error
    }

    @Test
    void timeoutEnUnServidorPasaAlSiguiente() {
        servidor.expect(requestTo(PRINCIPAL)).andRespond(withException(new SocketTimeoutException("Read timed out")));
        servidor.expect(requestTo(ESPEJO1)).andRespond(withSuccess(UN_LUGAR, MediaType.APPLICATION_JSON));

        assertThat(cliente.buscarLugares(39.6, -0.2, 500)).hasSize(1);
        servidor.verify();
    }

    @Test
    void demasiadasPeticionesPasaAlSiguiente() {
        servidor.expect(requestTo(PRINCIPAL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        servidor.expect(requestTo(ESPEJO1)).andRespond(withServerError());
        servidor.expect(requestTo(ESPEJO2)).andRespond(withSuccess(UN_LUGAR, MediaType.APPLICATION_JSON));

        assertThat(cliente.buscarLugares(39.6, -0.2, 500)).hasSize(1);
        servidor.verify();
    }

    @Test
    void respuestaIncompletaCuentaComoFallo() {
        servidor.expect(requestTo(PRINCIPAL)).andRespond(withSuccess("""
                {"remark": "runtime error: Query timed out in \\"query\\" at line 3 after 25 seconds.",
                 "elements": []}""", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(ESPEJO1)).andRespond(withSuccess(DOS_LUGARES, MediaType.APPLICATION_JSON));

        assertThat(cliente.buscarLugares(39.6, -0.2, 500)).hasSize(2);
        servidor.verify();
    }

    @Test
    void paginaHtmlEnVezDeJsonCuentaComoFallo() {
        servidor.expect(requestTo(PRINCIPAL)).andRespond(withSuccess("<html>Mantenimiento</html>", MediaType.TEXT_HTML));
        servidor.expect(requestTo(ESPEJO1)).andRespond(withSuccess(UN_LUGAR, MediaType.APPLICATION_JSON));

        assertThat(cliente.buscarLugares(39.6, -0.2, 500)).hasSize(1);
        servidor.verify();
    }

    @Test
    void siFallanTodosLanzaServicioMapasException(CapturedOutput salida) {
        servidor.expect(requestTo(PRINCIPAL)).andRespond(withServerError());
        servidor.expect(requestTo(ESPEJO1)).andRespond(withException(new SocketTimeoutException("Read timed out")));
        servidor.expect(requestTo(ESPEJO2)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> cliente.buscarLugares(39.6, -0.2, 500))
                .isInstanceOf(ServicioMapasException.class)
                .hasMessage("Servicio de mapas no disponible")
                .satisfies(e -> assertThat(e.getSuppressed()).hasSize(2));
        servidor.verify();
        assertThat(salida).contains("Fallaron los 3 servidores de Overpass");
    }

    // ---------- Pausa tras un fallo ----------

    @Test
    void unServidorQueFallaSePruebaElUltimoDuranteLaPausa() {
        // 1ª petición: el principal falla, responde el espejo 1.
        servidor.expect(requestTo(PRINCIPAL)).andRespond(withServerError());
        servidor.expect(requestTo(ESPEJO1)).andRespond(withSuccess(UN_LUGAR, MediaType.APPLICATION_JSON));
        // 2ª petición, un minuto después: va directa al espejo 1, sin esperar al principal.
        servidor.expect(requestTo(ESPEJO1)).andRespond(withSuccess(UN_LUGAR, MediaType.APPLICATION_JSON));

        cliente.buscarLugares(39.6, -0.2, 500);
        reloj.avanzar(Duration.ofMinutes(1));
        cliente.buscarLugares(39.6, -0.2, 500);

        servidor.verify();
        assertThat(cliente.ordenDeServidores()).containsExactly(ESPEJO1, ESPEJO2, PRINCIPAL);
    }

    @Test
    void trasLaPausaVuelveAlOrdenConfigurado() {
        servidor.expect(requestTo(PRINCIPAL)).andRespond(withServerError());
        servidor.expect(requestTo(ESPEJO1)).andRespond(withSuccess(UN_LUGAR, MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(PRINCIPAL)).andRespond(withSuccess(UN_LUGAR, MediaType.APPLICATION_JSON));

        cliente.buscarLugares(39.6, -0.2, 500);
        reloj.avanzar(OverpassClient.PAUSA_TRAS_FALLO.plusSeconds(1));
        cliente.buscarLugares(39.6, -0.2, 500);

        servidor.verify();
        assertThat(cliente.ordenDeServidores()).containsExactly(PRINCIPAL, ESPEJO1, ESPEJO2);
    }

    @Test
    void conTodosEnPausaSeSiguenProbandoComoUltimoRecurso() {
        servidor.expect(requestTo(PRINCIPAL)).andRespond(withServerError());
        servidor.expect(requestTo(ESPEJO1)).andRespond(withServerError());
        servidor.expect(requestTo(ESPEJO2)).andRespond(withServerError());
        // Siguiente petición: todos en pausa, se prueban igualmente en el orden configurado.
        servidor.expect(requestTo(PRINCIPAL)).andRespond(withSuccess(UN_LUGAR, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> cliente.buscarLugares(39.6, -0.2, 500)).isInstanceOf(ServicioMapasException.class);
        assertThat(cliente.buscarLugares(39.6, -0.2, 500)).hasSize(1);
        servidor.verify();
    }

    // ---------- Configuración ----------

    @Test
    void necesitaAlMenosUnServidor() {
        assertThatThrownBy(() -> new OverpassClient(RestClient.builder(), List.of(" ", ""), reloj))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overpass.urls");
    }

    @Test
    void ignoraEspaciosEnLaLista() {
        OverpassClient conEspacios = new OverpassClient(RestClient.builder(), List.of(" " + PRINCIPAL + " ", "", ESPEJO1), reloj);
        assertThat(conEspacios.ordenDeServidores()).containsExactly(PRINCIPAL, ESPEJO1);
    }

    /** Reloj que el test puede adelantar a mano. */
    private static final class RelojMovible extends Clock {
        private Instant ahora;

        RelojMovible(Instant inicio) {
            this.ahora = inicio;
        }

        void avanzar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public Instant instant() {
            return ahora;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
