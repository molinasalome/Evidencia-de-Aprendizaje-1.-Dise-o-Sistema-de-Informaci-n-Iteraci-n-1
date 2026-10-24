package com.autoscolombia.parqueadero.servicio;

import com.autoscolombia.parqueadero.modelo.*;
import com.autoscolombia.parqueadero.repositorio.MovimientoRepository;
import com.autoscolombia.parqueadero.repositorio.NovedadRepository;
import com.autoscolombia.parqueadero.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class NovedadService {

    private final NovedadRepository novedadRepository;
    private final MovimientoRepository movimientoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ValidacionPlacaService validacionPlacaService;

    public NovedadService(NovedadRepository novedadRepository,
                          MovimientoRepository movimientoRepository,
                          UsuarioRepository usuarioRepository,
                          ValidacionPlacaService validacionPlacaService) {
        this.novedadRepository = novedadRepository;
        this.movimientoRepository = movimientoRepository;
        this.usuarioRepository = usuarioRepository;
        this.validacionPlacaService = validacionPlacaService;
    }

    // Novedad manual del vigilante: solo si el vehículo tiene ingreso activo (CU-04)
    @Transactional
    public Novedad registrarNovedad(String placa, TipoNovedad tipo, String descripcion,
                                    String nombreUsuarioVigilante) {
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripción de la novedad es obligatoria.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Debe seleccionar el tipo de novedad.");
        }

        String p = validacionPlacaService.normalizar(placa);
        validacionPlacaService.determinarTipo(p);

        Movimiento movimiento = movimientoRepository
            .findByVehiculoPlacaAndFechaHoraSalidaIsNull(p)
            .orElseThrow(() -> new IllegalStateException(
                "El vehículo " + p + " no tiene un ingreso activo."));

        Empleado vigilante = usuarioRepository.buscarEmpleado(nombreUsuarioVigilante)
            .orElseThrow(() -> new IllegalStateException("El vigilante no existe."));

        Novedad novedad = new Novedad();
        novedad.setVehiculo(movimiento.getVehiculo());
        novedad.setMovimiento(movimiento);
        novedad.setEmpleado(vigilante);
        novedad.setTipo(tipo);
        novedad.setDescripcion(descripcion.trim());
        novedad.setFechaHora(LocalDateTime.now());
        return novedadRepository.save(novedad);
    }

    // Novedad automática cuando no hay celda libre (RF-09)
    @Transactional
    public Novedad registrarNovedadSinCelda(Movimiento movimiento) {
        Novedad novedad = new Novedad();
        novedad.setVehiculo(movimiento.getVehiculo());
        novedad.setMovimiento(movimiento);
        novedad.setEmpleado(movimiento.getVigilanteEntrada());
        novedad.setTipo(TipoNovedad.SIN_CELDA_DISPONIBLE);
        novedad.setDescripcion("No se encontró celda libre para el vehículo "
            + movimiento.getVehiculo().getPlaca() + ". Requiere gestión del administrador.");
        novedad.setFechaHora(LocalDateTime.now());
        return novedadRepository.save(novedad);
    }

    // Consulta con filtros opcionales: placa (parcial), tipo y rango de fechas (RF-18)
    @Transactional(readOnly = true)
    public List<Novedad> consultarNovedades(String placa, TipoNovedad tipo,
                                            LocalDate desde, LocalDate hasta) {
        String patronPlaca = "%" + validacionPlacaService.normalizar(placa) + "%";
        LocalDateTime inicio = (desde == null) ? null : desde.atStartOfDay();
        LocalDateTime fin = (hasta == null) ? null : hasta.atTime(LocalTime.MAX);
        return novedadRepository.listarPorFiltros(patronPlaca, tipo, inicio, fin);
    }
}