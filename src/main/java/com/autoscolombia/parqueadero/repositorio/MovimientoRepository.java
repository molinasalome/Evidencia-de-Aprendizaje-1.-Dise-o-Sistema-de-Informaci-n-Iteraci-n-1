package com.autoscolombia.parqueadero.repositorio;

import com.autoscolombia.parqueadero.modelo.Movimiento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MovimientoRepository extends JpaRepository<Movimiento, Integer> {

    // Movimiento activo = sin fecha de salida
    @EntityGraph(attributePaths = {"vehiculo", "celda"})
    Optional<Movimiento> findByVehiculoPlacaAndFechaHoraSalidaIsNull(String placa);

    @EntityGraph(attributePaths = {"vehiculo", "celda"})
    List<Movimiento> findByFechaHoraSalidaIsNull();

    // Historial: placa y usuario llegan como patrón ("%texto%"), las fechas pueden ser null
    @EntityGraph(attributePaths = {"vehiculo", "vehiculo.cliente", "celda"})
    @Query("""
           select m from Movimiento m
           where m.vehiculo.placa like :placa
             and lower(m.vehiculo.cliente.nombre) like :usuario
             and (:desde is null or m.fechaHoraEntrada >= :desde)
             and (:hasta is null or m.fechaHoraEntrada <= :hasta)
           order by m.fechaHoraEntrada desc
           """)
    List<Movimiento> listarPorFiltros(@Param("placa") String placa,
                                      @Param("usuario") String usuario,
                                      @Param("desde") LocalDateTime desde,
                                      @Param("hasta") LocalDateTime hasta);
}