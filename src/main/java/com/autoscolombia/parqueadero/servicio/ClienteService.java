package com.autoscolombia.parqueadero.servicio;

import com.autoscolombia.parqueadero.excepcion.DatosInvalidosException;
import com.autoscolombia.parqueadero.modelo.Cliente;
import com.autoscolombia.parqueadero.modelo.TipoVehiculo;
import com.autoscolombia.parqueadero.modelo.Vehiculo;
import com.autoscolombia.parqueadero.repositorio.ClienteRepository;
import com.autoscolombia.parqueadero.repositorio.MensualidadRepository;
import com.autoscolombia.parqueadero.repositorio.MovimientoRepository;
import com.autoscolombia.parqueadero.repositorio.UsuarioRepository;
import com.autoscolombia.parqueadero.repositorio.VehiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.autoscolombia.parqueadero.repositorio.NovedadRepository;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ClienteService {

    // Datos de un vehículo tal como llegan del formulario
    // (placaOriginal solo se usa al editar, para saber qué vehículo era)
    public record DatosVehiculo(String placaOriginal, String placa, TipoVehiculo tipo,
                                String marca, String color) {
    }

    // Datos del cliente tal como llegan del formulario
    public record DatosCliente(String nombre, String documento, String telefono,
                               String correo, List<DatosVehiculo> vehiculos) {
    }

    // Una fila del listado de clientes (CU-16)
    public record FilaCliente(Integer id, String nombre, String documento, String placas,
                              String estadoMensualidad, boolean activo) {
    }

        private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final VehiculoRepository vehiculoRepository;
    private final MensualidadRepository mensualidadRepository;
    private final MovimientoRepository movimientoRepository;
    private final NovedadRepository novedadRepository;   // NUEVO
    private final ValidacionPlacaService validacionPlacaService;

    public ClienteService(ClienteRepository clienteRepository,
                          UsuarioRepository usuarioRepository,
                          VehiculoRepository vehiculoRepository,
                          MensualidadRepository mensualidadRepository,
                          MovimientoRepository movimientoRepository,
                          NovedadRepository novedadRepository,   // NUEVO
                          ValidacionPlacaService validacionPlacaService) {
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.mensualidadRepository = mensualidadRepository;
        this.movimientoRepository = movimientoRepository;
        this.novedadRepository = novedadRepository;   // NUEVO
        this.validacionPlacaService = validacionPlacaService;
    }

    // CU-12 y CU-13: registrar un cliente con sus vehículos
    @Transactional
    public Cliente registrar(DatosCliente datos) {
        validarDatosBasicos(datos.nombre(), datos.documento());
        String documento = datos.documento().trim();

        if (usuarioRepository.existsByDocumento(documento)) {
            throw new DatosInvalidosException(
                "Ya existe un usuario con el documento " + documento + ".");
        }

        List<DatosVehiculo> vehiculos = datos.vehiculos() == null ? List.of() : datos.vehiculos();
        if (vehiculos.isEmpty()) {
            throw new DatosInvalidosException("Registra al menos un vehículo para el cliente.");
        }

        // Se validan todos los vehículos antes de guardar nada
        Set<String> vistas = new HashSet<>();
        for (DatosVehiculo v : vehiculos) {
            String placa = validarVehiculoNuevo(v);
            if (!vistas.add(placa)) {
                throw new DatosInvalidosException(
                    "La placa " + placa + " está repetida en el formulario.");
            }
        }

        Cliente cliente = new Cliente();
        cliente.setNombre(datos.nombre().trim());
        cliente.setDocumento(documento);
        cliente.setTelefono(limpiar(datos.telefono()));
        cliente.setCorreo(limpiar(datos.correo()));
        cliente.setActivo(true);
        clienteRepository.save(cliente);

        for (DatosVehiculo v : vehiculos) {
            guardarVehiculo(cliente, v);
        }
        return cliente;
    }

        // CU-17: editar los datos del cliente y de sus vehículos
    @Transactional
    public void editar(Integer idCliente, DatosCliente datos) {
        Cliente cliente = obtener(idCliente);
        validarDatosBasicos(datos.nombre(), datos.documento());
        String documento = datos.documento().trim();

        if (usuarioRepository.existsByDocumentoAndIdUsuarioNot(documento, idCliente)) {
            throw new DatosInvalidosException(
                "Ya existe un usuario con el documento " + documento + ".");
        }

        List<DatosVehiculo> vehiculos = datos.vehiculos() == null ? List.of() : datos.vehiculos();

        // Primero se valida todo; si algo falla, no se guarda nada
        Set<String> vistas = new HashSet<>();
        for (DatosVehiculo v : vehiculos) {
            String placa = validacionPlacaService.normalizar(v.placa());
            if (!vistas.add(placa)) {
                throw new DatosInvalidosException(
                    "La placa " + placa + " está repetida en el formulario.");
            }
            validarVehiculoEditado(cliente, v);
        }

        cliente.setNombre(datos.nombre().trim());
        cliente.setDocumento(documento);
        cliente.setTelefono(limpiar(datos.telefono()));
        cliente.setCorreo(limpiar(datos.correo()));
        clienteRepository.save(cliente);

        for (DatosVehiculo v : vehiculos) {
            aplicarVehiculoEditado(cliente, v);
        }
    }

    // CU-16: listado y búsqueda por nombre, documento o placa
    public List<FilaCliente> buscar(String texto) {
        List<Cliente> clientes;
        if (texto == null || texto.isBlank()) {
            clientes = clienteRepository.findAllByOrderByNombre();
        } else {
            String t = texto.trim();
            clientes = clienteRepository.buscar("%" + t.toLowerCase() + "%",
                                                "%" + t.toUpperCase() + "%");
        }
        return clientes.stream().map(this::aFila).toList();
    }

    // CU-18: activar o desactivar. Devuelve true si se desactivó a un cliente
    // que tiene un vehículo dentro (ese vehículo igual podrá salir).
    @Transactional
    public boolean cambiarEstado(Integer idCliente, boolean activo) {
        Cliente cliente = obtener(idCliente);
        cliente.setActivo(activo);
        clienteRepository.save(cliente);
        return !activo && movimientoRepository
            .existsByVehiculoClienteIdUsuarioAndFechaHoraSalidaIsNull(idCliente);
    }

    public Cliente obtener(Integer idCliente) {
        return clienteRepository.findById(idCliente)
            .orElseThrow(() -> new DatosInvalidosException("El cliente no existe."));
    }

    public List<Vehiculo> vehiculosDe(Integer idCliente) {
        return vehiculoRepository.findByClienteIdUsuarioOrderByPlaca(idCliente);
    }

    // Estado de la mensualidad: VIGENTE, VENCIDA o SIN_MENSUALIDAD
    public String estadoMensualidad(Integer idCliente) {
        if (!mensualidadRepository.buscarVigentes(idCliente, LocalDate.now()).isEmpty()) {
            return "VIGENTE";
        }
        boolean tuvoAlguna = mensualidadRepository
            .findFirstByClienteIdUsuarioOrderByFechaFinDesc(idCliente).isPresent();
        return tuvoAlguna ? "VENCIDA" : "SIN_MENSUALIDAD";
    }

        // ¿Se puede cambiar la placa? Solo si el vehículo no tiene movimientos ni novedades
    public boolean puedeCambiarPlaca(String placa) {
        String p = validacionPlacaService.normalizar(placa);
        return !movimientoRepository.existsByVehiculoPlaca(p)
            && !novedadRepository.existsByVehiculoPlaca(p);
    }

    // ---------- Auxiliares ----------

    private FilaCliente aFila(Cliente c) {
        String placas = vehiculosDe(c.getIdUsuario()).stream()
            .map(Vehiculo::getPlaca)
            .collect(Collectors.joining(", "));
        return new FilaCliente(c.getIdUsuario(), c.getNombre(), c.getDocumento(),
                               placas, estadoMensualidad(c.getIdUsuario()), c.isActivo());
    }

    private void validarDatosBasicos(String nombre, String documento) {
        if (nombre == null || nombre.isBlank() || documento == null || documento.isBlank()) {
            throw new DatosInvalidosException("Completa el nombre y el documento del cliente.");
        }
    }

    // Valida formato, tipo y que la placa no exista. Devuelve la placa normalizada.
    private String validarVehiculoNuevo(DatosVehiculo v) {
        String placa = validacionPlacaService.normalizar(v.placa());
        // Lanza PlacaInvalidaException si el formato no es de carro ni de moto
        TipoVehiculo esperado = validacionPlacaService.determinarTipo(placa);

        if (v.tipo() == null) {
            throw new DatosInvalidosException("Selecciona el tipo del vehículo " + placa + ".");
        }
        if (v.tipo() != esperado) {
            throw new DatosInvalidosException(
                "El tipo seleccionado no coincide con la placa " + placa
                + ": el formato corresponde a " + esperado.name().toLowerCase() + ".");
        }
        if (vehiculoRepository.existsById(placa)) {
            throw new DatosInvalidosException("La placa " + placa + " ya está registrada.");
        }
        return placa;
    }

        // Vehículo nuevo, igual o con placa cambiada: valida según el caso
    private void validarVehiculoEditado(Cliente cliente, DatosVehiculo v) {
        // Sin placa original: es un vehículo que se agrega ahora
        if (v.placaOriginal() == null || v.placaOriginal().isBlank()) {
            validarVehiculoNuevo(v);
            return;
        }

        String original = validacionPlacaService.normalizar(v.placaOriginal());
        String nueva = validacionPlacaService.normalizar(v.placa());

        Vehiculo actual = vehiculoRepository.findById(original)
            .orElseThrow(() -> new DatosInvalidosException(
                "El vehículo " + original + " no existe."));
        if (!actual.getCliente().getIdUsuario().equals(cliente.getIdUsuario())) {
            throw new DatosInvalidosException(
                "El vehículo " + original + " no pertenece a este cliente.");
        }

        // Cambio de placa: solo si el vehículo no tiene historia
        if (!nueva.equals(original)) {
            if (movimientoRepository.existsByVehiculoPlaca(original)
                    || novedadRepository.existsByVehiculoPlaca(original)) {
                throw new DatosInvalidosException(
                    "La placa " + original + " no se puede cambiar porque el vehículo "
                    + "tiene movimientos registrados.");
            }
            validarVehiculoNuevo(v);
        }
    }

    private void aplicarVehiculoEditado(Cliente cliente, DatosVehiculo v) {
        if (v.placaOriginal() == null || v.placaOriginal().isBlank()) {
            guardarVehiculo(cliente, v);
            return;
        }

        String original = validacionPlacaService.normalizar(v.placaOriginal());
        String nueva = validacionPlacaService.normalizar(v.placa());
        Vehiculo actual = vehiculoRepository.findById(original).orElseThrow();

        if (nueva.equals(original)) {
            // Misma placa: solo cambian marca y color
            actual.setMarca(limpiar(v.marca()));
            actual.setColor(limpiar(v.color()));
            vehiculoRepository.save(actual);
        } else {
            // La placa es la llave del vehículo: se reemplaza por uno nuevo
            vehiculoRepository.delete(actual);
            guardarVehiculo(cliente, v);
        }
    }

    private void guardarVehiculo(Cliente cliente, DatosVehiculo v) {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setPlaca(validacionPlacaService.normalizar(v.placa()));
        vehiculo.setTipo(v.tipo());
        vehiculo.setMarca(limpiar(v.marca()));
        vehiculo.setColor(limpiar(v.color()));
        vehiculo.setCliente(cliente);
        vehiculoRepository.save(vehiculo);
    }

    // Texto vacío se guarda como null
    private String limpiar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}