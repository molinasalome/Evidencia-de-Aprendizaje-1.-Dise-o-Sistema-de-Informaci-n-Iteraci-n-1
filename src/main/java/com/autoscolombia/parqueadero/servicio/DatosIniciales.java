package com.autoscolombia.parqueadero.servicio;

import com.autoscolombia.parqueadero.modelo.*;
import com.autoscolombia.parqueadero.repositorio.CeldaRepository;
import com.autoscolombia.parqueadero.repositorio.MensualidadRepository;
import com.autoscolombia.parqueadero.repositorio.UsuarioRepository;
import com.autoscolombia.parqueadero.repositorio.VehiculoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

// Solo para pruebas: se ejecuta únicamente con el perfil "local"
@Component
@Profile("local")
public class DatosIniciales implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final VehiculoRepository vehiculoRepository;
    private final MensualidadRepository mensualidadRepository;
    private final CeldaRepository celdaRepository;
    private final PasswordEncoder passwordEncoder;

    public DatosIniciales(UsuarioRepository usuarioRepository,
                          VehiculoRepository vehiculoRepository,
                          MensualidadRepository mensualidadRepository,
                          CeldaRepository celdaRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.mensualidadRepository = mensualidadRepository;
        this.celdaRepository = celdaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        crearEmpleado("admin", "Administrador Prueba", "1000000001", "Admin123", Rol.ADMINISTRADOR);
        crearEmpleado("vigilante1", "Vigilante Prueba", "1000000002", "Vigilante123", Rol.VIGILANTE);

        crearCeldas();

        LocalDate hoy = LocalDate.now();
        // Mensualidad vigente
        crearCliente("Juan Perez", "2000000001", "ABC123", TipoVehiculo.CARRO,
                     hoy.minusDays(10), hoy.plusDays(20));
        // Mensualidad vigente (moto)
        crearCliente("Maria Gomez", "2000000002", "XYZ12A", TipoVehiculo.MOTO,
                     hoy.minusDays(5), hoy.plusDays(25));
        // Mensualidad vencida
        crearCliente("Pedro Ruiz", "2000000003", "DEF456", TipoVehiculo.CARRO,
                     hoy.minusDays(60), hoy.minusDays(30));
    }

    private void crearEmpleado(String nombreUsuario, String nombre, String documento,
                               String contrasena, Rol rol) {
        if (usuarioRepository.buscarEmpleado(nombreUsuario).isPresent()) {
            return;
        }
        Empleado e = new Empleado();
        e.setNombre(nombre);
        e.setDocumento(documento);
        e.setNombreUsuario(nombreUsuario);
        e.setContrasenaHash(passwordEncoder.encode(contrasena));
        e.setRol(rol);
        usuarioRepository.save(e);
    }

    private void crearCeldas() {
        if (celdaRepository.count() > 0) {
            return;
        }
        for (int i = 1; i <= 5; i++) {
            crearCelda("C0" + i, TipoVehiculo.CARRO, EstadoCelda.LIBRE);
        }
        for (int i = 1; i <= 3; i++) {
            crearCelda("M0" + i, TipoVehiculo.MOTO, EstadoCelda.LIBRE);
        }
        // Una celda en mantenimiento, para probar el caso excepcional
        crearCelda("C06", TipoVehiculo.CARRO, EstadoCelda.MANTENIMIENTO);
    }

    private void crearCelda(String codigo, TipoVehiculo tipo, EstadoCelda estado) {
        Celda c = new Celda();
        c.setCodigo(codigo);
        c.setTipo(tipo);
        c.setEstado(estado);
        celdaRepository.save(c);
    }

    private void crearCliente(String nombre, String documento, String placa, TipoVehiculo tipo,
                              LocalDate inicio, LocalDate fin) {
        if (vehiculoRepository.existsById(placa)) {
            return;
        }
        Cliente cliente = new Cliente();
        cliente.setNombre(nombre);
        cliente.setDocumento(documento);
        cliente = (Cliente) usuarioRepository.save(cliente);

        Vehiculo v = new Vehiculo();
        v.setPlaca(placa);
        v.setTipo(tipo);
        v.setCliente(cliente);
        vehiculoRepository.save(v);

        Mensualidad m = new Mensualidad();
        m.setCliente(cliente);
        m.setFechaInicio(inicio);
        m.setFechaFin(fin);
        mensualidadRepository.save(m);
    }
}