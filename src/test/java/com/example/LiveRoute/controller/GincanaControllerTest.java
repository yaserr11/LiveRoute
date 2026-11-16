package com.example.LiveRoute.controller;

import com.example.LiveRoute.cliente.ServicioMapasException;
import com.example.LiveRoute.dto.GenerarGincanaRequest;
import com.example.LiveRoute.dto.GincanaGeneradaDTO;
import com.example.LiveRoute.dto.PrevisualizacionZonaDTO;
import com.example.LiveRoute.dto.RetoDTO;
import com.example.LiveRoute.repository.GincanaRepository;
import com.example.LiveRoute.service.GeneracionGincanaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la capa web: validación, códigos HTTP y forma del JSON. El servicio está simulado.
 */
@WebMvcTest(GincanaController.class)
class GincanaControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GeneracionGincanaService servicio;

    @MockitoBean
    private GincanaRepository gincanaRepository;

    private void generarDebeDar400(String json, String mensaje) throws Exception {
        mvc.perform(post("/api/gincanas/generar").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(mensaje));
        verify(servicio, never()).generar(any());
    }

    @Test
    void generarDevuelve201SinRespuesta() throws Exception {
        when(servicio.generar(any())).thenReturn(new GincanaGeneradaDTO(7L, "Gincana por Puerto de Sagunto",
                "Puerto de Sagunto", 39.6667, -0.2167, "media",
                List.of(new RetoDTO(100L, 1, "Busca una fuente. ¿Cómo se llama?", 39.6677, -0.2167))));

        mvc.perform(post("/api/gincanas/generar").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"latitud": 39.6667, "longitud": -0.2167}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.retos[0].pista").value("Busca una fuente. ¿Cómo se llama?"))
                .andExpect(jsonPath("$.retos[0].respuesta").doesNotExist())
                .andExpect(jsonPath("$.retos[0].gincana").doesNotExist());

        verify(servicio).generar(new GenerarGincanaRequest(39.6667, -0.2167, null, null, null));
    }

    @Test
    void generarValidaRadio() throws Exception {
        generarDebeDar400("""
                {"latitud": 39.6, "longitud": -0.2, "radioMetros": 199}""", "El radio debe ser de al menos 200 metros");
        generarDebeDar400("""
                {"latitud": 39.6, "longitud": -0.2, "radioMetros": 3001}""", "El radio no puede superar los 3000 metros");
    }

    @Test
    void generarValidaNumeroRetos() throws Exception {
        generarDebeDar400("""
                {"latitud": 39.6, "longitud": -0.2, "numeroRetos": 11}""", "No se pueden generar más de 10 retos");
        generarDebeDar400("""
                {"latitud": 39.6, "longitud": -0.2, "numeroRetos": 0}""", "Debe haber al menos 1 reto");
    }

    @Test
    void generarValidaCoordenadasYDificultad() throws Exception {
        generarDebeDar400("""
                {"longitud": -0.2}""", "La latitud es obligatoria");
        generarDebeDar400("""
                {"latitud": 91, "longitud": -0.2}""", "La latitud debe estar entre -90 y 90");
        generarDebeDar400("""
                {"latitud": 39.6, "longitud": -181}""", "La longitud debe estar entre -180 y 180");
        generarDebeDar400("""
                {"latitud": 39.6, "longitud": -0.2, "dificultad": "extrema"}""", "La dificultad debe ser facil, media o dificil");
        generarDebeDar400("{\"latitud\":", "El cuerpo de la petición no es válido");
    }

    @Test
    void generarSinLugaresDevuelve404() throws Exception {
        when(servicio.generar(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontraron lugares cerca"));

        mvc.perform(post("/api/gincanas/generar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitud\": 39.6, \"longitud\": -0.2}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No se encontraron lugares cerca"));
    }

    @Test
    void generarConMapaCaidoDevuelve502() throws Exception {
        when(servicio.generar(any())).thenThrow(new ServicioMapasException(new RuntimeException("timeout")));

        mvc.perform(post("/api/gincanas/generar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitud\": 39.6, \"longitud\": -0.2}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Servicio de mapas no disponible"));
    }

    @Test
    void previsualizarUsaRadioPorDefecto() throws Exception {
        when(servicio.previsualizar(39.6667, -0.2167, 1000)).thenReturn(new PrevisualizacionZonaDTO(12));

        mvc.perform(get("/api/gincanas/zona/previsualizar").param("latitud", "39.6667").param("longitud", "-0.2167"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lugaresDisponibles").value(12));
    }

    @Test
    void previsualizarValidaIgualQueGenerar() throws Exception {
        mvc.perform(get("/api/gincanas/zona/previsualizar")
                        .param("latitud", "39.6").param("longitud", "-0.2").param("radioMetros", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El radio debe ser de al menos 200 metros"));
        mvc.perform(get("/api/gincanas/zona/previsualizar").param("longitud", "-0.2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La latitud es obligatoria"));
        mvc.perform(get("/api/gincanas/zona/previsualizar").param("latitud", "abc").param("longitud", "-0.2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El parámetro latitud no es válido"));
        verify(servicio, never()).previsualizar(anyDouble(), anyDouble(), anyInt());
    }

    @Test
    void previsualizarConMapaCaidoDevuelve502() throws Exception {
        when(servicio.previsualizar(39.6, -0.2, 1000)).thenThrow(new ServicioMapasException(new RuntimeException()));

        mvc.perform(get("/api/gincanas/zona/previsualizar").param("latitud", "39.6").param("longitud", "-0.2"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Servicio de mapas no disponible"));
    }
}
