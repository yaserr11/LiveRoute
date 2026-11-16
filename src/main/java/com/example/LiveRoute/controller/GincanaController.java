package com.example.LiveRoute.controller;

import com.example.LiveRoute.dto.GenerarGincanaRequest;
import com.example.LiveRoute.dto.GincanaGeneradaDTO;
import com.example.LiveRoute.dto.PrevisualizacionZonaDTO;
import com.example.LiveRoute.model.Gincana;
import com.example.LiveRoute.repository.GincanaRepository;
import com.example.LiveRoute.service.GeneracionGincanaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.LiveRoute.dto.LimitesZona.*;

@RestController
@RequestMapping("/api/gincanas")
public class GincanaController {

    private final GincanaRepository gincanaRepository;
    private final GeneracionGincanaService generacionService;

    public GincanaController(GincanaRepository gincanaRepository, GeneracionGincanaService generacionService) {
        this.gincanaRepository = gincanaRepository;
        this.generacionService = generacionService;
    }

    @GetMapping
    public List<Gincana> listar() {
        return gincanaRepository.findAll();
    }

    @PostMapping
    public Gincana crear(@RequestBody Gincana gincana) {
        return gincanaRepository.save(gincana);
    }

    @PostMapping("/generar")
    @ResponseStatus(HttpStatus.CREATED)
    public GincanaGeneradaDTO generar(@Valid @RequestBody GenerarGincanaRequest peticion) {
        return generacionService.generar(peticion);
    }

    /** No guarda nada: solo dice cuántos lugares hay en la zona. Mismos límites que "generar". */
    @GetMapping("/zona/previsualizar")
    public PrevisualizacionZonaDTO previsualizar(
            @RequestParam(required = false)
            @NotNull(message = MSG_LATITUD_OBLIGATORIA)
            @DecimalMin(value = "-90", message = MSG_LATITUD_RANGO)
            @DecimalMax(value = "90", message = MSG_LATITUD_RANGO)
            Double latitud,

            @RequestParam(required = false)
            @NotNull(message = MSG_LONGITUD_OBLIGATORIA)
            @DecimalMin(value = "-180", message = MSG_LONGITUD_RANGO)
            @DecimalMax(value = "180", message = MSG_LONGITUD_RANGO)
            Double longitud,

            @RequestParam(defaultValue = "" + RADIO_POR_DEFECTO)
            @Min(value = RADIO_MIN, message = MSG_RADIO_MIN)
            @Max(value = RADIO_MAX, message = MSG_RADIO_MAX)
            int radioMetros) {
        return generacionService.previsualizar(latitud, longitud, radioMetros);
    }
}
