package com.autoscolombia.parqueadero.servicio;

import com.autoscolombia.parqueadero.excepcion.IngresoActivoException;
import com.autoscolombia.parqueadero.excepcion.SinCeldaDisponibleException;
import com.autoscolombia.parqueadero.modelo.Celda;
import com.autoscolombia.parqueadero.modelo.Empleado;
import com.autoscolombia.parqueadero.modelo.Movimiento;
import com.autoscolombia.parqueadero.modelo.TipoVehiculo;
import com.autoscolombia.parqueadero.modelo.Vehiculo;
import com.autoscolombia.parqueadero.repositorio.MovimientoRepository;
import com.autoscolombia.parqueadero.repositorio.UsuarioRepository;
import com.autoscolombia.parqueadero.repositorio.VehiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class MovimientoService {

    private final MovimientoRepository movimientoRepository;
    private final VehiculoRepository vehiculoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ValidacionPlacaService validacionPlacaService;
    private final UsuarioService usuarioService;
    private final CeldaService celdaService;
    private final NovedadService novedadService;

    public MovimientoService(MovimientoRepository movimientoRepository,
                             VehiculoRepository vehiculoRepository,
                             UsuarioRepository usuarioRepository,
                             ValidacionPlacaService validacionPlacaService,
                             UsuarioService usuarioService,
                             CeldaService celdaService,
                             NovedadService novedadService) {
        this.movimientoRepository = movimientoRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.usuarioRepository = usuarioRepository;
        this.validacionPlacaService = validacionPlacaService;
        this.usuarioService = usuarioService;
        this.celdaService = celdaService;
        this.novedadService = novedadService;
    }

    // CU-02: registrar la entrada de un vehículo
    @Transactional
    public Movimiento registrarEntrada(String placa, String nombreUsuarioVigilante) {
        String p = validacionPlacaService.normalizar(placa);

        // 1. Formato de la placa (lanza PlacaInvalidaException si es inválido)
        validacionPlacaService.determinarTipo(p);

        // 2. Placa registrada y mensualidad vigente (CU-03)
        usuarioService.validarIngreso(p);

        // 3. Que no tenga un ingreso activo (CU-04)
        if (movimientoRepository.findByVehiculoPlacaAndFechaHoraSalidaIsNull(p).isPresent()) {
            throw new IngresoActivoException("El vehículo " + p + " ya está en el parqueadero.");
        }

        Vehiculo vehiculo = vehiculoRepository.findByPlaca(p).orElseThrow();
        Empleado vigilante = buscarVigilante(nombreUsuarioVigilante);

        Movimiento movimiento = new Movimiento();
        movimiento.setVehiculo(vehiculo);
        movimiento.setVigilanteEntrada(vigilante);
        movimiento.setFechaHoraEntrada(LocalDateTime.now());

        // 4. Asignar celda (CU-05). Si no hay, entra sin celda y queda una novedad (RF-09)
        boolean sinCelda = false;
        try {
            Celda celda = celdaService.asignarCelda(vehiculo.getTipo());
            movimiento.setCelda(celda);
        } catch (SinCeldaDisponibleException e) {
            sinCelda = true;
        }

        movimiento = movimientoRepository.save(movimiento);

        if (sinCelda) {
            novedadService.registrarNovedadSinCelda(movimiento);
        }
        return movimiento;
    }

    // CU-06: registrar la salida de un vehículo
    @Transactional
    public Movimiento registrarSalida(String placa, String nombreUsuarioVigilante) {
        String p = validacionPlacaService.normalizar(placa);

        // Primero el formato de la placa (lanza PlacaInvalidaException si es inválido)
        validacionPlacaService.determinarTipo(p);

        Movimiento movimiento = movimientoRepository
            .findByVehiculoPlacaAndFechaHoraSalidaIsNull(p)
            .orElseThrow(() -> new IllegalStateException(
                "El vehículo " + p + " no tiene una entrada activa."));

        Empleado vigilante = buscarVigilante(nombreUsuarioVigilante);

        // Guarda fecha, hora, vigilante y tiempo de permanencia en minutos
        movimiento.registrarSalida(LocalDateTime.now(), vigilante);

        // Libera la celda (si tenía una)
        if (movimiento.getCelda() != null) {
            celdaService.liberarCelda(movimiento.getCelda().getIdCelda());
        }
        return movimientoRepository.save(movimiento);
    }

    // CU-08: vehículos que están dentro del parqueadero
    @Transactional(readOnly = true)
    public List<Movimiento> consultarVehiculosDentro() {
        return movimientoRepository.findByFechaHoraSalidaIsNull();
    }

        // CU-09: historial con filtros opcionales (placa, nombre del usuario, rango de fechas)
    @Transactional(readOnly = true)
    public List<Movimiento> consultarHistorial(String placa, String usuario,
                                               LocalDate desde, LocalDate hasta) {
        String patronPlaca = "%" + validacionPlacaService.normalizar(placa) + "%";
        String patronUsuario = "%" + (usuario == null ? "" : usuario.trim().toLowerCase()) + "%";
        LocalDateTime inicio = (desde == null) ? null : desde.atStartOfDay();
        LocalDateTime fin = (hasta == null) ? null : hasta.atTime(LocalTime.MAX);
        return movimientoRepository.listarPorFiltros(patronPlaca, patronUsuario, inicio, fin);
    }

    // Datos listos para mostrar en la pantalla "Vehículos dentro"
    public record VehiculoDentro(String placa, TipoVehiculo tipo, String usuario, String celda,
                                 LocalDateTime horaEntrada, long minutosDentro) {

        public String tipoTexto() {
            return tipo == TipoVehiculo.CARRO ? "Carro" : "Moto";
        }

        // Solo la hora si entró hoy; si entró otro día, se antepone la fecha
        public String horaEntradaTexto() {
            Locale es = Locale.forLanguageTag("es-CO");
            String hora = horaEntrada.format(DateTimeFormatter.ofPattern("h:mm a", es));
            if (horaEntrada.toLocalDate().equals(LocalDate.now())) {
                return hora;
            }
            return horaEntrada.format(DateTimeFormatter.ofPattern("dd/MM", es)) + " " + hora;
        }

        public String tiempoDentroTexto() {
            long horas = minutosDentro / 60;
            long minutos = minutosDentro % 60;
            return horas > 0 ? horas + " h " + minutos + " min" : minutos + " min";
        }
    }

    // CU-08: vehículos dentro, con usuario, celda y tiempo (más antiguos primero)
    @Transactional(readOnly = true)
    public List<VehiculoDentro> consultarVehiculosDentroDetalle() {
        LocalDateTime ahora = LocalDateTime.now();
        return movimientoRepository.findByFechaHoraSalidaIsNull().stream()
            .sorted(Comparator.comparing(Movimiento::getFechaHoraEntrada))
            .map(m -> new VehiculoDentro(
                m.getVehiculo().getPlaca(),
                m.getVehiculo().getTipo(),
                m.getVehiculo().getCliente().getNombre(),
                m.getCelda() != null ? m.getCelda().getCodigo() : "Sin celda",
                m.getFechaHoraEntrada(),
                Duration.between(m.getFechaHoraEntrada(), ahora).toMinutes()))
            .toList();
    }

    private Empleado buscarVigilante(String nombreUsuario) {
        return usuarioRepository.buscarEmpleado(nombreUsuario)
            .orElseThrow(() -> new IllegalStateException("El vigilante no existe."));
    }
}