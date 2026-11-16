package com.example.LiveRoute.service;

import com.example.LiveRoute.dto.RetoDTO;
import com.example.LiveRoute.model.Gincana;
import com.example.LiveRoute.model.Reto;
import com.example.LiveRoute.repository.GincanaRepository;
import com.example.LiveRoute.repository.RetoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

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
        return normalizar(respuesta).equals(normalizar(reto.getRespuesta()));
    }

    /**
     * Quita tildes, espacios y mayúsculas: "Plaza Málaga" -> "plazamalaga".
     */
    static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private Gincana buscarGincana(Long gincanaId) {
        return gincanaRepository.findById(gincanaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gincana no encontrada"));
    }
}
