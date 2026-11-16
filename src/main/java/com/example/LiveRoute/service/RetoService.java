package com.example.LiveRoute.service;

import com.example.LiveRoute.model.Gincana;
import com.example.LiveRoute.model.Reto;
import com.example.LiveRoute.repository.GincanaRepository;
import com.example.LiveRoute.repository.RetoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RetoService {

    private final RetoRepository retoRepository;
    private final GincanaRepository gincanaRepository;

    public RetoService(RetoRepository retoRepository, GincanaRepository gincanaRepository) {
        this.retoRepository = retoRepository;
        this.gincanaRepository = gincanaRepository;
    }

    public List<Reto> listar(Long gincanaId) {
        return retoRepository.findByGincanaIdOrderByOrdenAsc(gincanaId);
    }

    public Reto crear(Long gincanaId, Reto reto) {
        reto.setGincana(buscarGincana(gincanaId));
        return retoRepository.save(reto);
    }

    private Gincana buscarGincana(Long gincanaId) {
        return gincanaRepository.findById(gincanaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gincana no encontrada"));
    }
}
