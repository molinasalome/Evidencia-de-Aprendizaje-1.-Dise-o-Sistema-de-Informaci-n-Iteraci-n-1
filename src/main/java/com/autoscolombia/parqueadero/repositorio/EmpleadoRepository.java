package com.autoscolombia.parqueadero.repositorio;

import com.autoscolombia.parqueadero.modelo.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmpleadoRepository extends JpaRepository<Empleado, Integer> {

    List<Empleado> findAllByOrderByNombre();
}