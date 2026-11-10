package com.example.LiveRoute.controller;

import com.example.LiveRoute.model.Gincana;
import com.example.LiveRoute.repository.GincanaRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gincanas")
public class GincanaController {

    private final GincanaRepository gincanaRepository;

    public GincanaController(GincanaRepository gincanaRepository) {
        this.gincanaRepository = gincanaRepository;
    }

    @GetMapping
    public List<Gincana> listar() {
        return gincanaRepository.findAll();
    }

    @PostMapping
    public Gincana crear(@RequestBody Gincana gincana) {
        return gincanaRepository.save(gincana);
    }
}