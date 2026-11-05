package com.autoscolombia.parqueadero.controlador;

import com.autoscolombia.parqueadero.excepcion.DatosInvalidosException;
import com.autoscolombia.parqueadero.excepcion.PlacaInvalidaException;
import com.autoscolombia.parqueadero.modelo.Cliente;
import com.autoscolombia.parqueadero.modelo.TipoVehiculo;
import com.autoscolombia.parqueadero.servicio.ClienteService;
import com.autoscolombia.parqueadero.servicio.ClienteService.DatosCliente;
import com.autoscolombia.parqueadero.servicio.ClienteService.DatosVehiculo;
import com.autoscolombia.parqueadero.servicio.ClienteService.FilaCliente;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

// Gestión de clientes: solo el administrador (la ruta /admin/** ya está protegida)
@Controller
@RequestMapping("/admin/clientes")
public class ClienteController {

    private static final int POR_PAGINA = 6;

    // Un vehículo tal como se muestra en el formulario
    public record FilaVehiculo(String placaOriginal, String placa, String tipo,
                               String marca, String color, boolean bloqueada) {
    }

    // Lo que se muestra en el formulario (también sirve para volver a pintarlo si hay error)
    public record FormCliente(String nombre, String documento, String telefono,
                              String correo, List<FilaVehiculo> vehiculos) {
    }

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    // ---------- CU-16: listado y búsqueda ----------

    @GetMapping
    public String listar(@RequestParam(required = false) String q,
                         @RequestParam(defaultValue = "1") int pagina,
                         Model modelo) {

        List<FilaCliente> todos = clienteService.buscar(q);

        int totalPaginas = Math.max(1, (int) Math.ceil(todos.size() / (double) POR_PAGINA));
        int actual = Math.min(Math.max(pagina, 1), totalPaginas);
        int inicio = (actual - 1) * POR_PAGINA;
        List<FilaCliente> visibles = todos.subList(inicio, Math.min(inicio + POR_PAGINA, todos.size()));

        modelo.addAttribute("filas", visibles);
        modelo.addAttribute("total", todos.size());
        modelo.addAttribute("paginaActual", actual);
        modelo.addAttribute("totalPaginas", totalPaginas);
        modelo.addAttribute("q", q == null ? "" : q.trim());
        return "admin-clientes";
    }

    // ---------- CU-18: activar o desactivar ----------

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Integer id,
                                @RequestParam boolean activo,
                                RedirectAttributes mensajes) {
        try {
            boolean tieneVehiculoDentro = clienteService.cambiarEstado(id, activo);
            mensajes.addFlashAttribute("exito", activo
                ? "Cliente activado correctamente."
                : "Cliente desactivado correctamente.");
            if (tieneVehiculoDentro) {
                mensajes.addFlashAttribute("alerta",
                    "El cliente tiene un vehículo dentro del parqueadero. "
                    + "Podrá registrar su salida normalmente.");
            }
        } catch (DatosInvalidosException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/clientes";
    }

    // ---------- CU-12 y CU-13: registrar cliente ----------

    @GetMapping("/nuevo")
    public String nuevo(Model modelo) {
        FormCliente vacio = new FormCliente("", "", "", "",
            List.of(new FilaVehiculo("", "", "", "", "", false)));
        cargarFormulario(modelo, null, vacio);
        return "admin-cliente-form";
    }

    @PostMapping("/nuevo")
    public String guardarNuevo(@RequestParam String nombre,
                               @RequestParam String documento,
                               @RequestParam(required = false) String telefono,
                               @RequestParam(required = false) String correo,
                               @RequestParam(required = false) List<String> placaOriginal,
                               @RequestParam(required = false) List<String> placa,
                               @RequestParam(required = false) List<String> tipo,
                               @RequestParam(required = false) List<String> marca,
                               @RequestParam(required = false) List<String> color,
                               Model modelo,
                               RedirectAttributes mensajes) {

        List<FilaVehiculo> filas = armarFilas(placaOriginal, placa, tipo, marca, color);
        FormCliente form = new FormCliente(nombre, documento, telefono, correo, filas);
        try {
            clienteService.registrar(aDatos(form));
            mensajes.addFlashAttribute("exito", "Cliente registrado correctamente.");
            return "redirect:/admin/clientes";
        } catch (DatosInvalidosException | PlacaInvalidaException e) {
            modelo.addAttribute("error", e.getMessage());
            cargarFormulario(modelo, null, form);
            return "admin-cliente-form";
        }
    }

    // ---------- CU-17: editar cliente ----------

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Integer id, Model modelo, RedirectAttributes mensajes) {
        try {
            Cliente c = clienteService.obtener(id);
            List<FilaVehiculo> filas = clienteService.vehiculosDe(id).stream()
                .map(v -> new FilaVehiculo(v.getPlaca(), v.getPlaca(), v.getTipo().name(),
                    nz(v.getMarca()), nz(v.getColor()),
                    !clienteService.puedeCambiarPlaca(v.getPlaca())))
                .toList();
            cargarFormulario(modelo, id, new FormCliente(
                c.getNombre(), c.getDocumento(), nz(c.getTelefono()), nz(c.getCorreo()), filas));
            return "admin-cliente-form";
        } catch (DatosInvalidosException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/clientes";
        }
    }

    @PostMapping("/{id}/editar")
    public String guardarEdicion(@PathVariable Integer id,
                                 @RequestParam String nombre,
                                 @RequestParam String documento,
                                 @RequestParam(required = false) String telefono,
                                 @RequestParam(required = false) String correo,
                                 @RequestParam(required = false) List<String> placaOriginal,
                                 @RequestParam(required = false) List<String> placa,
                                 @RequestParam(required = false) List<String> tipo,
                                 @RequestParam(required = false) List<String> marca,
                                 @RequestParam(required = false) List<String> color,
                                 Model modelo,
                                 RedirectAttributes mensajes) {

        List<FilaVehiculo> filas = armarFilas(placaOriginal, placa, tipo, marca, color);
        FormCliente form = new FormCliente(nombre, documento, telefono, correo, filas);
        try {
            clienteService.editar(id, aDatos(form));
            mensajes.addFlashAttribute("exito", "Cliente actualizado correctamente.");
            return "redirect:/admin/clientes";
        } catch (DatosInvalidosException | PlacaInvalidaException e) {
            modelo.addAttribute("error", e.getMessage());
            cargarFormulario(modelo, id, form);
            return "admin-cliente-form";
        }
    }

    // ---------- Auxiliares ----------

    private void cargarFormulario(Model modelo, Integer id, FormCliente form) {
        boolean editando = id != null;
        modelo.addAttribute("titulo", editando ? "Editar cliente" : "Registrar cliente");
        modelo.addAttribute("accion", editando
            ? "/admin/clientes/" + id + "/editar"
            : "/admin/clientes/nuevo");
        modelo.addAttribute("textoBoton", editando ? "Guardar cambios" : "Guardar cliente");
        modelo.addAttribute("editando", editando);
        modelo.addAttribute("form", form);
    }

    // Junta las listas del formulario (una posición por vehículo) en filas.
    // Las filas completamente vacías que son nuevas se descartan.
    private List<FilaVehiculo> armarFilas(List<String> placaOriginal, List<String> placa,
                                          List<String> tipo, List<String> marca,
                                          List<String> color) {
        List<FilaVehiculo> filas = new ArrayList<>();
        int total = placa == null ? 0 : placa.size();
        for (int i = 0; i < total; i++) {
            String original = valor(placaOriginal, i);
            String p = valor(placa, i);
            String t = valor(tipo, i);
            String m = valor(marca, i);
            String c = valor(color, i);

            boolean esNueva = original.isBlank();
            if (esNueva && p.isBlank() && m.isBlank() && c.isBlank()) {
                continue;
            }
            boolean bloqueada = !esNueva && !clienteService.puedeCambiarPlaca(original);
            filas.add(new FilaVehiculo(original, p, t, m, c, bloqueada));
        }
        return filas;
    }

    private DatosCliente aDatos(FormCliente form) {
        List<DatosVehiculo> vehiculos = form.vehiculos().stream()
            .map(f -> new DatosVehiculo(f.placaOriginal(), f.placa(),
                f.tipo().isBlank() ? null : TipoVehiculo.valueOf(f.tipo()),
                f.marca(), f.color()))
            .toList();
        return new DatosCliente(form.nombre(), form.documento(), form.telefono(),
                                form.correo(), vehiculos);
    }

    private String valor(List<String> lista, int i) {
        return (lista != null && i < lista.size() && lista.get(i) != null) ? lista.get(i) : "";
    }

    private String nz(String texto) {
        return texto == null ? "" : texto;
    }
}