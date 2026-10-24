package com.autoscolombia.parqueadero.servicio;

import com.autoscolombia.parqueadero.modelo.Empleado;
import com.autoscolombia.parqueadero.repositorio.UsuarioRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Devuelve el empleado si las credenciales son correctas; si no, rechaza sin dar pistas
    public Empleado autenticar(String nombreUsuario, String contrasena) {
        Empleado empleado = usuarioRepository.buscarEmpleado(nombreUsuario)
            .orElseThrow(() -> new BadCredentialsException("Usuario o contraseña incorrectos."));

        if (!empleado.isActivo()
                || !passwordEncoder.matches(contrasena, empleado.getContrasenaHash())) {
            throw new BadCredentialsException("Usuario o contraseña incorrectos.");
        }
        return empleado;
    }
}