package com.autoscolombia.parqueadero.repositorio;

import com.autoscolombia.parqueadero.modelo.Celda;
import com.autoscolombia.parqueadero.modelo.EstadoCelda;
import com.autoscolombia.parqueadero.modelo.TipoVehiculo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface CeldaRepository extends JpaRepository<Celda, Integer> {

    // Bloqueo de escritura: evita que dos entradas simultáneas tomen la misma celda (RNF-05)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Celda> findFirstByTipoAndEstadoOrderByIdCeldaAsc(TipoVehiculo tipo, EstadoCelda estado);

    long countByTipoAndEstado(TipoVehiculo tipo, EstadoCelda estado);

    long countByTipo(TipoVehiculo tipo);

    List<Celda> findByTipoOrderByIdCeldaAsc(TipoVehiculo tipo);
}