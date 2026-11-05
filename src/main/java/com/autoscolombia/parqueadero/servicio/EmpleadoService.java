package com.autoscolombia.parqueadero.servicio;

import com.autoscolombia.parqueadero.excepcion.DatosInvalidosException;
import com.autoscolombia.parqueadero.modelo.Empleado;
import com.autoscolombia.parqueadero.modelo.Rol;
import com.autoscolombia.parqueadero.repositorio.EmpleadoRepository;
import com.autoscolombia.parqueadero.repositorio.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public EmpleadoService(EmpleadoRepository empleadoRepository,
                           UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder) {
        this.empleadoRepository = empleadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Empleado> listar() {
        return empleadoRepository.findAllByOrderByNombre();
    }

    // CU-19: registrar un empleado. La contraseña se guarda cifrada (RF-27).
    @Transactional
    public void registrar(String nombre, String documento, String nombreUsuario,
                          Rol rol, String contrasena) {

        if (esVacio(nombre) || esVacio(documento) || esVacio(nombreUsuario)
                || rol == null || esVacio(contrasena)) {
            throw new DatosInvalidosException("Completa todos los campos obligatorios.");
        }

        String doc = documento.trim();
        String usuario = nombreUsuario.trim();

        if (usuarioRepository.existsByDocumento(doc)) {
            throw new DatosInvalidosException("Ya existe un usuario con ese documento.");
        }
        if (usuarioRepository.buscarEmpleado(usuario).isPresent()) {
            throw new DatosInvalidosException("Ese nombre de usuario ya está en uso.");
        }

        Empleado e = new Empleado();
        e.setNombre(nombre.trim());
        e.setDocumento(doc);
        e.setNombreUsuario(usuario);
        e.setContrasenaHash(passwordEncoder.encode(contrasena));
        e.setRol(rol);
        e.setActivo(true);
        empleadoRepository.save(e);
    }

    private boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}