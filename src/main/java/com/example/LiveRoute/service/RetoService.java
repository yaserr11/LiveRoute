package com.example.LiveRoute.service;

import com.example.LiveRoute.dto.RetoDTO;
import com.example.LiveRoute.model.Gincana;
import com.example.LiveRoute.model.Reto;
import com.example.LiveRoute.repository.GincanaRepository;
import com.example.LiveRoute.repository.RetoRepository;
import com.example.LiveRoute.util.Texto;
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

    public List<RetoDTO> listar(Long gincanaId) {
        buscarGincana(gincanaId);
        return retoRepository.findByGincanaIdOrderByOrdenAsc(gincanaId).stream()
                .map(RetoDTO::desde)
                .toList();
    }

    public Reto crear(Long gincanaId, Reto reto) {
        reto.setGincana(buscarGincana(gincanaId));
        return retoRepository.save(reto);
    }

    public boolean comprobar(Long gincanaId, Long retoId, String respuesta) {
        if (respuesta == null || respuesta.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La respuesta es obligatoria");
        }
        buscarGincana(gincanaId);
        Reto reto = retoRepository.findByIdAndGincanaId(retoId, gincanaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reto no encontrado"));
        if (reto.getRespuesta() == null) {
            return false;
        }
        return Texto.normalizar(respuesta).equals(Texto.normalizar(reto.getRespuesta()));
    }

    private Gincana buscarGincana(Long gincanaId) {
        return gincanaRepository.findById(gincanaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gincana no encontrada"));
    }
}
