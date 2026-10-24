package com.autoscolombia.parqueadero.servicio;

import com.autoscolombia.parqueadero.modelo.Empleado;
import com.autoscolombia.parqueadero.repositorio.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DetalleEmpleadoService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public DetalleEmpleadoService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String nombreUsuario) throws UsernameNotFoundException {
        Empleado empleado = usuarioRepository.buscarEmpleado(nombreUsuario)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario o contraseña incorrectos."));

        return User.builder()
            .username(empleado.getNombreUsuario())
            .password(empleado.getContrasenaHash())
            .roles(empleado.getRol().name())   // VIGILANTE o ADMINISTRADOR
            .disabled(!empleado.isActivo())
            .build();
    }
}