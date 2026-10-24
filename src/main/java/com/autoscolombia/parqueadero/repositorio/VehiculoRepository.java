package com.autoscolombia.parqueadero.repositorio;

import com.autoscolombia.parqueadero.modelo.Vehiculo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehiculoRepository extends JpaRepository<Vehiculo, String> {

    @EntityGraph(attributePaths = {"cliente"})
    Optional<Vehiculo> findByPlaca(String placa);
}