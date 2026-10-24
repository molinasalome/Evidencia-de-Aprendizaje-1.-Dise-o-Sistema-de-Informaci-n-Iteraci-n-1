package com.autoscolombia.parqueadero.servicio;

import com.autoscolombia.parqueadero.excepcion.SinCeldaDisponibleException;
import com.autoscolombia.parqueadero.modelo.Celda;
import com.autoscolombia.parqueadero.modelo.EstadoCelda;
import com.autoscolombia.parqueadero.modelo.Movimiento;
import com.autoscolombia.parqueadero.modelo.TipoVehiculo;
import com.autoscolombia.parqueadero.repositorio.CeldaRepository;
import com.autoscolombia.parqueadero.repositorio.MovimientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CeldaService {

    private final CeldaRepository celdaRepository;
    private final MovimientoRepository movimientoRepository;

    public CeldaService(CeldaRepository celdaRepository, MovimientoRepository movimientoRepository) {
        this.celdaRepository = celdaRepository;
        this.movimientoRepository = movimientoRepository;
    }

    // Busca una celda libre compatible con el tipo y la marca como ocupada (RNF-05)
    @Transactional(noRollbackFor = SinCeldaDisponibleException.class)
    public Celda asignarCelda(TipoVehiculo tipo) {
        Celda celda = celdaRepository
            .findFirstByTipoAndEstadoOrderByIdCeldaAsc(tipo, EstadoCelda.LIBRE)
            .orElseThrow(() -> new SinCeldaDisponibleException(
                "No hay celda libre disponible para " + tipo.name().toLowerCase() + "."));
        celda.ocupar();
        return celdaRepository.save(celda);
    }

    // Deja la celda disponible de inmediato (RF-14)
    @Transactional
    public void liberarCelda(Integer idCelda) {
        Celda celda = celdaRepository.findById(idCelda)
            .orElseThrow(() -> new IllegalArgumentException("La celda no existe."));
        celda.liberar();
        celdaRepository.save(celda);
    }

    // Totales de celdas, separados por carro y moto (RF-19)
    @Transactional(readOnly = true)
    public DisponibilidadCeldas obtenerDisponibilidad() {
        return new DisponibilidadCeldas(
            celdaRepository.countByTipoAndEstado(TipoVehiculo.CARRO, EstadoCelda.LIBRE),
            celdaRepository.countByTipoAndEstado(TipoVehiculo.CARRO, EstadoCelda.OCUPADA),
            celdaRepository.countByTipo(TipoVehiculo.CARRO),
            celdaRepository.countByTipoAndEstado(TipoVehiculo.MOTO, EstadoCelda.LIBRE),
            celdaRepository.countByTipoAndEstado(TipoVehiculo.MOTO, EstadoCelda.OCUPADA),
            celdaRepository.countByTipo(TipoVehiculo.MOTO));
    }

    public record DisponibilidadCeldas(long carrosLibres, long carrosOcupadas, long carrosTotal,
                                       long motosLibres, long motosOcupadas, long motosTotal) {

        public long totalCeldas() { return carrosTotal + motosTotal; }
        public long libres() { return carrosLibres + motosLibres; }
        public long ocupadas() { return carrosOcupadas + motosOcupadas; }
    }

    // Una celda lista para dibujar; si está ocupada trae placa, usuario y hora de entrada
    public record CeldaVista(String codigo, EstadoCelda estado, String placa, String usuario,
                             LocalDateTime desde) {

        public String estadoClase() {
            return switch (estado) {
                case LIBRE -> "libre";
                case OCUPADA -> "ocupada";
                case MANTENIMIENTO -> "mantenimiento";
            };
        }

        public String estadoTexto() {
            return switch (estado) {
                case LIBRE -> "Libre";
                case OCUPADA -> "Ocupada";
                case MANTENIMIENTO -> "En mantenimiento";
            };
        }

        // Solo la hora si entró hoy; si entró otro día, se antepone la fecha
        public String desdeTexto() {
            if (desde == null) {
                return "";
            }
            Locale es = Locale.forLanguageTag("es-CO");
            String hora = desde.format(DateTimeFormatter.ofPattern("h:mm a", es));
            if (desde.toLocalDate().equals(LocalDate.now())) {
                return hora;
            }
            return desde.format(DateTimeFormatter.ofPattern("dd/MM", es)) + " " + hora;
        }
    }

    // Todas las celdas separadas por tipo, con sus totales
    public record MapaCeldas(List<CeldaVista> carros, List<CeldaVista> motos) {

        private long contar(EstadoCelda estado) {
            return carros.stream().filter(c -> c.estado() == estado).count()
                + motos.stream().filter(c -> c.estado() == estado).count();
        }

        public long libres() { return contar(EstadoCelda.LIBRE); }
        public long ocupadas() { return contar(EstadoCelda.OCUPADA); }
        public long mantenimiento() { return contar(EstadoCelda.MANTENIMIENTO); }

        public long carrosLibres() {
            return carros.stream().filter(c -> c.estado() == EstadoCelda.LIBRE).count();
        }

        public long motosLibres() {
            return motos.stream().filter(c -> c.estado() == EstadoCelda.LIBRE).count();
        }
    }

    // RF-19: estado de cada celda, con el vehículo que la ocupa
    @Transactional(readOnly = true)
    public MapaCeldas obtenerMapa() {
        // Movimientos activos por celda (una celda solo puede tener uno activo)
        Map<Integer, Movimiento> activos = movimientoRepository.findByFechaHoraSalidaIsNull().stream()
            .filter(m -> m.getCelda() != null)
            .collect(Collectors.toMap(m -> m.getCelda().getIdCelda(), m -> m));

        return new MapaCeldas(
            vistas(TipoVehiculo.CARRO, activos),
            vistas(TipoVehiculo.MOTO, activos));
    }

    private List<CeldaVista> vistas(TipoVehiculo tipo, Map<Integer, Movimiento> activos) {
        return celdaRepository.findByTipoOrderByIdCeldaAsc(tipo).stream()
            .map(c -> {
                Movimiento m = activos.get(c.getIdCelda());
                if (m == null || c.getEstado() != EstadoCelda.OCUPADA) {
                    return new CeldaVista(c.getCodigo(), c.getEstado(), null, null, null);
                }
                return new CeldaVista(c.getCodigo(), c.getEstado(),
                    m.getVehiculo().getPlaca(),
                    m.getVehiculo().getCliente().getNombre(),
                    m.getFechaHoraEntrada());
            })
            .toList();
    }
}