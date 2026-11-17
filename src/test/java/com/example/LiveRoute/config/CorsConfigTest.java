package com.example.LiveRoute.config;

import com.example.LiveRoute.controller.GincanaController;
import com.example.LiveRoute.repository.GincanaRepository;
import com.example.LiveRoute.service.GeneracionGincanaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprueba que solo el front end configurado puede llamar a la API desde el navegador.
 */
@WebMvcTest(GincanaController.class)
class CorsConfigTest {

    private static final String FRONT_END = "http://localhost:5173";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private GincanaRepository gincanaRepository;

    @MockitoBean
    private GeneracionGincanaService generacionService;

    @Test
    void elFrontEndConfiguradoRecibeLasCabecerasCors() throws Exception {
        when(gincanaRepository.findAll()).thenReturn(List.of());

        mvc.perform(get("/api/gincanas").header("Origin", FRONT_END))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", FRONT_END));
    }

    @Test
    void laPeticionPreviaDelNavegadorSeAceptaParaPost() throws Exception {
        mvc.perform(options("/api/gincanas/generar")
                        .header("Origin", FRONT_END)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", FRONT_END));
    }

    @Test
    void otroOrigenSeRechaza() throws Exception {
        mvc.perform(get("/api/gincanas").header("Origin", "http://otra-web.com"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
