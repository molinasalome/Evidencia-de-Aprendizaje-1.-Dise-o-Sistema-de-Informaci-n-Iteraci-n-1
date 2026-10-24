package com.autoscolombia.parqueadero.repositorio;

import com.autoscolombia.parqueadero.modelo.Mensualidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MensualidadRepository extends JpaRepository<Mensualidad, Integer> {

    @Query("""
           select m from Mensualidad m
           where m.cliente.idUsuario = :idCliente
             and :fecha between m.fechaInicio and m.fechaFin
           order by m.fechaFin desc
           """)
    List<Mensualidad> buscarVigentes(@Param("idCliente") Integer idCliente,
                                     @Param("fecha") LocalDate fecha);
}