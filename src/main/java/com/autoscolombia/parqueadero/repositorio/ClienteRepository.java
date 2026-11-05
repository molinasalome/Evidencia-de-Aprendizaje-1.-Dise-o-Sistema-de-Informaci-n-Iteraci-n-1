package com.autoscolombia.parqueadero.repositorio;

import com.autoscolombia.parqueadero.modelo.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    // Busca por nombre, documento o placa. Los dos parámetros llegan como patrón ("%texto%"):
    // "texto" en minúscula para el nombre y "placa" en mayúscula para la placa.
    @Query("""
           select c from Cliente c
           where lower(c.nombre) like :texto
              or c.documento like :texto
              or exists (select v from Vehiculo v
                         where v.cliente = c and v.placa like :placa)
           order by c.nombre
           """)
    List<Cliente> buscar(@Param("texto") String texto, @Param("placa") String placa);

    List<Cliente> findAllByOrderByNombre();
}