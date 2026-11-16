package com.example.LiveRoute.controller;

import com.example.LiveRoute.dto.RespuestaRequest;
import com.example.LiveRoute.dto.ResultadoComprobacion;
import com.example.LiveRoute.dto.RetoDTO;
import com.example.LiveRoute.model.Reto;
import com.example.LiveRoute.service.RetoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gincanas/{gincanaId}/retos")
public class RetoController {

    private final RetoService retoService;

    public RetoController(RetoService retoService) {
        this.retoService = retoService;
    }

    @GetMapping
    public List<RetoDTO> listar(@PathVariable Long gincanaId) {
        return retoService.listar(gincanaId);
    }

    @PostMapping
    public Reto crear(@PathVariable Long gincanaId, @RequestBody Reto reto) {
        return retoService.crear(gincanaId, reto);
    }

    @PostMapping("/{retoId}/comprobar")
    public ResultadoComprobacion comprobar(@PathVariable Long gincanaId,
                                           @PathVariable Long retoId,
                                           @RequestBody RespuestaRequest request) {
        return new ResultadoComprobacion(retoService.comprobar(gincanaId, retoId, request.respuesta()));
    }
}
