package com.example.LiveRoute.controller;

import com.example.LiveRoute.cliente.ServicioMapasException;
import com.example.LiveRoute.dto.ErrorDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Convierte los errores en respuestas {"error": "mensaje"} con el código HTTP adecuado.
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    /** Cuerpo JSON con campos no válidos (@Valid @RequestBody). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDTO> cuerpoNoValido(MethodArgumentNotValidException e) {
        return error(HttpStatus.BAD_REQUEST, unirMensajes(e.getBindingResult().getAllErrors()));
    }

    /** Parámetros de la URL no válidos (@RequestParam con anotaciones de validación). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorDTO> parametrosNoValidos(HandlerMethodValidationException e) {
        return error(HttpStatus.BAD_REQUEST, unirMensajes(e.getAllErrors()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDTO> jsonNoValido(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDTO> tipoNoValido(MethodArgumentTypeMismatchException e) {
        return error(HttpStatus.BAD_REQUEST, "El parámetro " + e.getName() + " no es válido");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorDTO> faltaParametro(MissingServletRequestParameterException e) {
        return error(HttpStatus.BAD_REQUEST, "Falta el parámetro " + e.getParameterName());
    }

    @ExceptionHandler(ServicioMapasException.class)
    public ResponseEntity<ErrorDTO> servicioMapas(ServicioMapasException e) {
        log.warn("Fallo del servicio de mapas: {}", e.getCause() != null ? e.getCause().toString() : e.getMessage());
        return error(HttpStatus.BAD_GATEWAY, "Servicio de mapas no disponible");
    }

    /** Errores lanzados por los servicios (404 "Gincana no encontrada", etc.). */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorDTO> conEstado(ResponseStatusException e) {
        String mensaje = e.getReason() != null ? e.getReason() : e.getStatusCode().toString();
        return error(e.getStatusCode(), mensaje);
    }

    private static String unirMensajes(Collection<? extends MessageSourceResolvable> errores) {
        return errores.stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .distinct()
                .sorted()
                .collect(Collectors.joining(". "));
    }

    private static ResponseEntity<ErrorDTO> error(HttpStatusCode estado, String mensaje) {
        return ResponseEntity.status(estado).body(new ErrorDTO(mensaje));
    }
}
