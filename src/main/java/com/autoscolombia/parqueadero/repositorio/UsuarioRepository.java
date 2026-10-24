package com.autoscolombia.parqueadero.repositorio;

import com.autoscolombia.parqueadero.modelo.Empleado;
import com.autoscolombia.parqueadero.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    @Query("select e from Empleado e where e.nombreUsuario = :nombreUsuario")
    Optional<Empleado> buscarEmpleado(@Param("nombreUsuario") String nombreUsuario);
}