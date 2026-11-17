package com.example.LiveRoute.service;

import com.example.LiveRoute.cliente.GeocodificadorInverso;
import com.example.LiveRoute.cliente.LugarCercano;
import com.example.LiveRoute.cliente.ProveedorLugares;
import com.example.LiveRoute.cliente.ServicioMapasException;
import com.example.LiveRoute.cliente.TipoLugar;
import com.example.LiveRoute.dto.GenerarGincanaRequest;
import com.example.LiveRoute.dto.GincanaGeneradaDTO;
import com.example.LiveRoute.dto.RetoDTO;
import com.example.LiveRoute.model.Gincana;
import com.example.LiveRoute.model.Reto;
import com.example.LiveRoute.repository.GincanaRepository;
import com.example.LiveRoute.repository.RetoRepository;
import com.example.LiveRoute.util.Texto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests del servicio con el proveedor de mapas simulado: nunca se llama a Overpass de verdad.
 */
class GeneracionGincanaServiceTest {

    private static final double LAT = 39.6667;
    private static final double LON = -0.2167;

    private ProveedorLugares proveedor;
    private GeocodificadorInverso geocodificador;
    private GincanaRepository gincanaRepository;
    private RetoRepository retoRepository;
    private GeneracionGincanaService servicio;

    @BeforeEach
    void preparar() {
        proveedor = mock(ProveedorLugares.class);
        geocodificador = mock(GeocodificadorInverso.class);
        gincanaRepository = mock(GincanaRepository.class);
        retoRepository = mock(RetoRepository.class);
        TransactionTemplate transaccion = new TransactionTemplate(mock(PlatformTransactionManager.class));
        servicio = new GeneracionGincanaService(proveedor, geocodificador, gincanaRepository, retoRepository,
                new GeneradorPistas(), transaccion);

        when(geocodificador.nombreZona(anyDouble(), anyDouble())).thenReturn(Optional.of("Puerto de Sagunto"));
        when(gincanaRepository.save(any(Gincana.class))).thenAnswer(inv -> {
            Gincana g = inv.getArgument(0);
            g.setId(7L);
            return g;
        });
        when(retoRepository.saveAll(any())).thenAnswer(inv -> {
            List<Reto> retos = new ArrayList<>();
            long id = 100;
            for (Reto r : inv.<Iterable<Reto>>getArgument(0)) {
                r.setId(id++);
                retos.add(r);
            }
            return retos;
        });
    }

    /** Lugar desplazado desde el centro (1 milésima de grado de latitud ≈ 111 m). */
    private static LugarCercano lugar(String nombre, TipoLugar tipo, double dLat, double dLon) {
        return new LugarCercano(nombre, tipo, LAT + dLat, LON + dLon);
    }

    private static GenerarGincanaRequest peticion(Integer radio, Integer numeroRetos, String dificultad) {
        return new GenerarGincanaRequest(LAT, LON, radio, numeroRetos, dificultad);
    }

    @SuppressWarnings("unchecked")
    private List<Reto> retosGuardados() {
        ArgumentCaptor<Iterable<Reto>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(retoRepository).saveAll(captor.capture());
        List<Reto> retos = new ArrayList<>();
        captor.getValue().forEach(retos::add);
        return retos;
    }

    @Test
    void ordenaYendoSiempreAlLugarMasCercano() {
        // Ordenar solo por distancia al centro daría Norte1, Sur, Norte2.
        // Yendo siempre al más cercano desde el último: Norte1 -> Norte2 -> Sur.
        when(proveedor.buscarLugares(LAT, LON, 1000)).thenReturn(List.of(
                lugar("Sur", TipoLugar.PARQUE, -0.0015, 0),
                lugar("Norte2", TipoLugar.FUENTE, 0.002, 0),
                lugar("Norte1", TipoLugar.MIRADOR, 0.001, 0)));

        servicio.generar(peticion(1000, 5, "media"));

        List<Reto> retos = retosGuardados();
        assertThat(retos).extracting(Reto::getRespuesta).containsExactly("Norte1", "Norte2", "Sur");
        assertThat(retos).extracting(Reto::getOrden).containsExactly(1, 2, 3);
    }

    @Test
    void creaComoMaximoNumeroRetos() {
        when(proveedor.buscarLugares(LAT, LON, 1000)).thenReturn(List.of(
                lugar("A", TipoLugar.FUENTE, 0.001, 0),
                lugar("B", TipoLugar.FUENTE, 0.002, 0),
                lugar("C", TipoLugar.FUENTE, 0.003, 0),
                lugar("D", TipoLugar.FUENTE, 0.004, 0)));

        GincanaGeneradaDTO resultado = servicio.generar(peticion(1000, 2, "media"));

        assertThat(resultado.retos()).hasSize(2);
        assertThat(retosGuardados()).extracting(Reto::getRespuesta).containsExactly("A", "B");
    }

    @Test
    void conMenosLugaresQueRetosCreaLosQueHay() {
        when(proveedor.buscarLugares(LAT, LON, 1000)).thenReturn(List.of(
                lugar("A", TipoLugar.FUENTE, 0.001, 0),
                lugar("B", TipoLugar.PARQUE, 0.002, 0)));

        GincanaGeneradaDTO resultado = servicio.generar(peticion(1000, 10, "media"));

        assertThat(resultado.retos()).hasSize(2);
    }

    @Test
    void sinLugaresDevuelve404YNoGuardaNada() {
        when(proveedor.buscarLugares(LAT, LON, 1000)).thenReturn(List.of());

        assertThatThrownBy(() -> servicio.generar(peticion(1000, 5, "media")))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(e.getReason()).isEqualTo("No se encontraron lugares cerca");
                });
        verify(gincanaRepository, never()).save(any());
    }

    @Test
    void siFallaElMapaNoGuardaNada() {
        when(proveedor.buscarLugares(anyDouble(), anyDouble(), anyInt()))
                .thenThrow(new ServicioMapasException(new RuntimeException("timeout")));

        assertThatThrownBy(() -> servicio.generar(peticion(1000, 5, "media")))
                .isInstanceOf(ServicioMapasException.class)
                .hasMessage("Servicio de mapas no disponible");
        verify(gincanaRepository, never()).save(any());
    }

    @Test
    void aplicaValoresPorDefecto() {
        List<LugarCercano> muchos = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            muchos.add(lugar("Lugar " + i, TipoLugar.MONUMENTO, i * 0.001, 0));
        }
        when(proveedor.buscarLugares(LAT, LON, 1000)).thenReturn(muchos);

        GincanaGeneradaDTO resultado = servicio.generar(peticion(null, null, null));

        verify(proveedor).buscarLugares(LAT, LON, 1000);
        assertThat(resultado.retos()).hasSize(5);
        assertThat(resultado.dificultad()).isEqualTo("media");
    }

    @Test
    void usaElRadioYCentroRecibidos() {
        when(proveedor.buscarLugares(40.4168, -3.7038, 2500))
                .thenReturn(List.of(new LugarCercano("Plaza Mayor", TipoLugar.SITIO_HISTORICO, 40.4155, -3.7074)));

        GincanaGeneradaDTO resultado = servicio.generar(new GenerarGincanaRequest(40.4168, -3.7038, 2500, 5, "media"));

        verify(proveedor).buscarLugares(40.4168, -3.7038, 2500);
        assertThat(resultado.latitud()).isEqualTo(40.4168);
        assertThat(resultado.longitud()).isEqualTo(-3.7038);
    }

    @Test
    void tituloYLugarConNombreDeZona() {
        when(proveedor.buscarLugares(LAT, LON, 1000)).thenReturn(List.of(lugar("A", TipoLugar.FUENTE, 0.001, 0)));

        GincanaGeneradaDTO resultado = servicio.generar(peticion(1000, 5, "facil"));

        assertThat(resultado.id()).isEqualTo(7L);
        assertThat(resultado.titulo()).isEqualTo("Gincana por Puerto de Sagunto");
        assertThat(resultado.lugar()).isEqualTo("Puerto de Sagunto");
        assertThat(resultado.dificultad()).isEqualTo("facil");
    }

    @Test
    void sinNombreDeZonaUsaCoordenadas() {
        when(geocodificador.nombreZona(anyDouble(), anyDouble())).thenReturn(Optional.empty());
        when(proveedor.buscarLugares(LAT, LON, 1000)).thenReturn(List.of(lugar("A", TipoLugar.FUENTE, 0.001, 0)));

        GincanaGeneradaDTO resultado = servicio.generar(peticion(1000, 5, "media"));

        assertThat(resultado.lugar()).isEqualTo("Zona 39.6667, -0.2167");
        assertThat(resultado.titulo()).isEqualTo("Gincana por Zona 39.6667, -0.2167");
    }

    @Test
    void guardaLaRespuestaPeroLaPistaNoContieneElNombre() {
        when(proveedor.buscarLugares(LAT, LON, 1000)).thenReturn(List.of(
                lugar("Fuente de la Plaza", TipoLugar.FUENTE, 0.001, 0),
                lugar("Parque Central", TipoLugar.PARQUE, 0.002, 0.001)));

        GincanaGeneradaDTO resultado = servicio.generar(peticion(1000, 5, "facil"));

        for (Reto reto : retosGuardados()) {
            assertThat(reto.getRespuesta()).isNotBlank();
            assertThat(Texto.normalizar(reto.getPista())).doesNotContain(Texto.normalizar(reto.getRespuesta()));
        }
        assertThat(RetoDTO.class.getRecordComponents())
                .extracting(c -> c.getName())
                .doesNotContain("respuesta", "gincana");
        assertThat(resultado.retos()).extracting(RetoDTO::id).containsExactly(100L, 101L);
    }

    @Test
    void previsualizarCuentaLugaresSinGuardar() {
        when(proveedor.buscarLugares(LAT, LON, 500)).thenReturn(List.of(
                lugar("A", TipoLugar.FUENTE, 0.001, 0),
                lugar("B", TipoLugar.PARQUE, 0.002, 0)));

        assertThat(servicio.previsualizar(LAT, LON, 500).lugaresDisponibles()).isEqualTo(2);
        verify(gincanaRepository, never()).save(any());
        verify(retoRepository, never()).saveAll(any());
    }
}
