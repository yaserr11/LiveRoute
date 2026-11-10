package com.example.LiveRoute.controller;

import com.example.LiveRoute.model.Gincana;
import com.example.LiveRoute.model.Reto;
import com.example.LiveRoute.repository.GincanaRepository;
import com.example.LiveRoute.repository.RetoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/gincanas/{gincanaId}/retos")
public class RetoController {

    private final RetoRepository retoRepository;
    private final GincanaRepository gincanaRepository;

    public RetoController(RetoRepository retoRepository, GincanaRepository gincanaRepository) {
        this.retoRepository = retoRepository;
        this.gincanaRepository = gincanaRepository;
    }

    @GetMapping
    public List<Reto> listar(@PathVariable Long gincanaId) {
        return retoRepository.findByGincanaIdOrderByOrdenAsc(gincanaId);
    }

    @PostMapping
    public Reto crear(@PathVariable Long gincanaId, @RequestBody Reto reto) {
        Gincana gincana = gincanaRepository.findById(gincanaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gincana no encontrada"));
        reto.setGincana(gincana);
        return retoRepository.save(reto);
    }
}