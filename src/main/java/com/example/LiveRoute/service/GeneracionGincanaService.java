package com.example.LiveRoute.service;

import com.example.LiveRoute.cliente.GeocodificadorInverso;
import com.example.LiveRoute.cliente.LugarCercano;
import com.example.LiveRoute.cliente.ProveedorLugares;
import com.example.LiveRoute.dto.GenerarGincanaRequest;
import com.example.LiveRoute.dto.GincanaGeneradaDTO;
import com.example.LiveRoute.dto.PrevisualizacionZonaDTO;
import com.example.LiveRoute.dto.RetoDTO;
import com.example.LiveRoute.model.Gincana;
import com.example.LiveRoute.model.Reto;
import com.example.LiveRoute.repository.GincanaRepository;
import com.example.LiveRoute.repository.RetoRepository;
import com.example.LiveRoute.util.Geo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static com.example.LiveRoute.dto.LimitesZona.*;

/**
 * Genera gincanas a partir de lugares reales cercanos al punto que elige el jugador.
 */
@Service
public class GeneracionGincanaService {

    private final ProveedorLugares proveedorLugares;
    private final GeocodificadorInverso geocodificador;
    private final GincanaRepository gincanaRepository;
    private final RetoRepository retoRepository;
    private final GeneradorPistas generadorPistas;
    private final TransactionTemplate transaccion;

    public GeneracionGincanaService(ProveedorLugares proveedorLugares,
                                    GeocodificadorInverso geocodificador,
                                    GincanaRepository gincanaRepository,
                                    RetoRepository retoRepository,
                                    GeneradorPistas generadorPistas,
                                    TransactionTemplate transaccion) {
        this.proveedorLugares = proveedorLugares;
        this.geocodificador = geocodificador;
        this.gincanaRepository = gincanaRepository;
        this.retoRepository = retoRepository;
        this.generadorPistas = generadorPistas;
        this.transaccion = transaccion;
    }

    /** Cuenta los lugares de la zona sin guardar nada. */
    public PrevisualizacionZonaDTO previsualizar(double latitud, double longitud, int radioMetros) {
        return new PrevisualizacionZonaDTO(proveedorLugares.buscarLugares(latitud, longitud, radioMetros).size());
    }

    public GincanaGeneradaDTO generar(GenerarGincanaRequest peticion) {
        double latitud = peticion.latitud();
        double longitud = peticion.longitud();
        int radio = peticion.radioMetros() != null ? peticion.radioMetros() : RADIO_POR_DEFECTO;
        int numeroRetos = peticion.numeroRetos() != null ? peticion.numeroRetos() : RETOS_POR_DEFECTO;
        String dificultad = peticion.dificultad() != null ? peticion.dificultad() : DIFICULTAD_POR_DEFECTO;

        // 1. Llamadas externas, fuera de la transacción para no tener la base de datos esperando.
        List<LugarCercano> lugares = proveedorLugares.buscarLugares(latitud, longitud, radio);
        if (lugares.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontraron lugares cerca");
        }
        List<LugarCercano> ruta = ordenarPorCercania(lugares, latitud, longitud, numeroRetos);
        String zona = geocodificador.nombreZona(latitud, longitud)
                .orElseGet(() -> String.format(Locale.ROOT, "Zona %.4f, %.4f", latitud, longitud));

        // 2. Construir la gincana y sus retos.
        Gincana gincana = new Gincana();
        gincana.setTitulo("Gincana por " + zona);
        gincana.setLugar(zona);
        gincana.setLatitud(latitud);
        gincana.setLongitud(longitud);
        gincana.setDificultad(dificultad);

        List<Reto> retos = new ArrayList<>();
        double anteriorLat = latitud;
        double anteriorLon = longitud;
        for (int i = 0; i < ruta.size(); i++) {
            LugarCercano lugar = ruta.get(i);
            double distancia = Geo.distanciaMetros(anteriorLat, anteriorLon, lugar.latitud(), lugar.longitud());
            String direccion = Geo.direccion(anteriorLat, anteriorLon, lugar.latitud(), lugar.longitud());

            Reto reto = new Reto();
            reto.setGincana(gincana);
            reto.setOrden(i + 1);
            reto.setPista(generadorPistas.crear(lugar.tipo(), dificultad, distancia, direccion, i == 0));
            reto.setRespuesta(lugar.nombre());
            reto.setLatitud(lugar.latitud());
            reto.setLongitud(lugar.longitud());
            retos.add(reto);

            anteriorLat = lugar.latitud();
            anteriorLon = lugar.longitud();
        }

        // 3. Guardar todo junto: si algo falla no queda una gincana a medias.
        List<Reto> guardados = transaccion.execute(estado -> {
            gincanaRepository.save(gincana);
            return retoRepository.saveAll(retos);
        });

        return new GincanaGeneradaDTO(gincana.getId(), gincana.getTitulo(), gincana.getLugar(),
                gincana.getLatitud(), gincana.getLongitud(), gincana.getDificultad(),
                guardados.stream().map(RetoDTO::desde).toList());
    }

    /**
     * Orden de paseo: empieza por el lugar más cercano al punto de inicio y después
     * va siempre al lugar más cercano que aún no se ha visitado. Se queda con los primeros "maximo".
     */
    static List<LugarCercano> ordenarPorCercania(List<LugarCercano> lugares, double latitud, double longitud, int maximo) {
        List<LugarCercano> pendientes = new ArrayList<>(lugares);
        List<LugarCercano> ruta = new ArrayList<>();
        double actualLat = latitud;
        double actualLon = longitud;
        while (!pendientes.isEmpty() && ruta.size() < maximo) {
            final double desdeLat = actualLat;
            final double desdeLon = actualLon;
            LugarCercano siguiente = pendientes.stream()
                    .min(Comparator.comparingDouble(l -> Geo.distanciaMetros(desdeLat, desdeLon, l.latitud(), l.longitud())))
                    .orElseThrow();
            pendientes.remove(siguiente);
            ruta.add(siguiente);
            actualLat = siguiente.latitud();
            actualLon = siguiente.longitud();
        }
        return ruta;
    }
}
