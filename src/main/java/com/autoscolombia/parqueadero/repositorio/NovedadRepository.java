package com.autoscolombia.parqueadero.repositorio;

import com.autoscolombia.parqueadero.modelo.Novedad;
import com.autoscolombia.parqueadero.modelo.TipoNovedad;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NovedadRepository extends JpaRepository<Novedad, Integer> {

    // La placa llega como patrón ("%texto%"); tipo y fechas pueden ser null
    @EntityGraph(attributePaths = {"vehiculo", "empleado"})
    @Query("""
           select n from Novedad n
           where n.vehiculo.placa like :placa
             and (:tipo is null or n.tipo = :tipo)
             and (:desde is null or n.fechaHora >= :desde)
             and (:hasta is null or n.fechaHora <= :hasta)
           order by n.fechaHora desc
           """)
    List<Novedad> listarPorFiltros(@Param("placa") String placa,
                                   @Param("tipo") TipoNovedad tipo,
                                   @Param("desde") LocalDateTime desde,
                                   @Param("hasta") LocalDateTime hasta);
}