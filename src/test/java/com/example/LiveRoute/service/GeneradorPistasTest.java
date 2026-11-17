package com.example.LiveRoute.service;

import com.example.LiveRoute.cliente.TipoLugar;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeneradorPistasTest {

    private final GeneradorPistas generador = new GeneradorPistas();

    @Test
    void facilDaDistanciaYDireccion() {
        String pista = generador.crear(TipoLugar.FUENTE, "facil", 260, "noreste", false);
        assertThat(pista).isEqualTo(
                "Busca una fuente donde la gente se refresca. Está a unos 250 m al noreste del reto anterior. ¿Cómo se llama?");
    }

    @Test
    void mediaDaSoloDistancia() {
        String pista = generador.crear(TipoLugar.PARQUE, "media", 410, "sur", false);
        assertThat(pista).contains("a unos 400 m del reto anterior").doesNotContain("sur");
    }

    @Test
    void dificilSoloDaLaPlantilla() {
        String pista = generador.crear(TipoLugar.MIRADOR, "dificil", 410, "sur", false);
        assertThat(pista).isEqualTo("Busca un mirador con buenas vistas. ¿Cómo se llama?");
    }

    @Test
    void primerRetoSeMideDesdeElInicio() {
        assertThat(generador.crear(TipoLugar.MUSEO, "media", 30, "este", true))
                .contains("a menos de 50 m del punto de inicio");
    }

    @Test
    void todosLosTiposTienenPlantilla() {
        for (TipoLugar tipo : TipoLugar.values()) {
            assertThat(generador.crear(tipo, "dificil", 100, "norte", false)).startsWith("Busca ");
        }
    }
}
