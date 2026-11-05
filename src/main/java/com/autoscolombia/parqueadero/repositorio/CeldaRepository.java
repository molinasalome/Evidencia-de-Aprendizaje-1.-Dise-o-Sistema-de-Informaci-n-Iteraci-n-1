package com.autoscolombia.parqueadero.repositorio;

import com.autoscolombia.parqueadero.modelo.Celda;
import com.autoscolombia.parqueadero.modelo.EstadoCelda;
import com.autoscolombia.parqueadero.modelo.TipoVehiculo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CeldaRepository extends JpaRepository<Celda, Integer> {

    // Bloqueo de escritura: evita que dos entradas simultáneas tomen la misma celda (RNF-05)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Celda> findFirstByTipoAndEstadoOrderByIdCeldaAsc(TipoVehiculo tipo, EstadoCelda estado);

    long countByTipoAndEstado(TipoVehiculo tipo, EstadoCelda estado);

    long countByTipo(TipoVehiculo tipo);

    List<Celda> findByTipoOrderByIdCeldaAsc(TipoVehiculo tipo);

    
    // Código repetido al registrar y al editar (se ignora la propia celda)
    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdCeldaNot(String codigo, Integer idCelda);

    // CU-23: listado con filtros opcionales por tipo y estado
    @Query("""
           select c from Celda c
           where (:tipo is null or c.tipo = :tipo)
             and (:estado is null or c.estado = :estado)
           order by c.codigo
           """)
    List<Celda> filtrar(@Param("tipo") TipoVehiculo tipo,
                        @Param("estado") EstadoCelda estado);

    // Busca la celda bloqueándola: si en ese momento entra un vehículo, uno espera al otro
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Celda> findByIdCelda(Integer idCelda);
}