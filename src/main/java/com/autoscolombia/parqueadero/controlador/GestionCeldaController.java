package com.autoscolombia.parqueadero.controlador;

import com.autoscolombia.parqueadero.excepcion.DatosInvalidosException;
import com.autoscolombia.parqueadero.modelo.Celda;
import com.autoscolombia.parqueadero.modelo.EstadoCelda;
import com.autoscolombia.parqueadero.modelo.TipoVehiculo;
import com.autoscolombia.parqueadero.servicio.GestionCeldaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Gestión de celdas: solo el administrador (la ruta /admin/** ya está protegida)
@Controller
@RequestMapping("/admin/celdas")
public class GestionCeldaController {

    private final GestionCeldaService gestionCeldaService;

    public GestionCeldaController(GestionCeldaService gestionCeldaService) {
        this.gestionCeldaService = gestionCeldaService;
    }

    // CU-23: listado con filtros
    @GetMapping
    public String listar(@RequestParam(required = false) TipoVehiculo tipo,
                         @RequestParam(required = false) EstadoCelda estado,
                         Model modelo) {
        cargarListado(modelo, tipo, estado);
        cargarFormulario(modelo, false, null, "", "");
        return "admin-celdas";
    }

    // CU-20: registrar
    @PostMapping
    public String registrar(@RequestParam(required = false) String codigo,
                            @RequestParam(required = false) TipoVehiculo tipo,
                            Model modelo,
                            RedirectAttributes mensajes) {
        try {
            gestionCeldaService.registrar(codigo, tipo);
            mensajes.addFlashAttribute("exito", "Celda registrada correctamente.");
            return "redirect:/admin/celdas";
        } catch (DatosInvalidosException e) {
            cargarListado(modelo, null, null);
            cargarFormulario(modelo, true, null, codigo, tipo == null ? "" : tipo.name());
            modelo.addAttribute("error", e.getMessage());
            return "admin-celdas";
        }
    }

    // CU-21: abrir el formulario con los datos de la celda
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Integer id, Model modelo, RedirectAttributes mensajes) {
        try {
            Celda celda = gestionCeldaService.obtener(id);
            if (celda.getEstado() == EstadoCelda.OCUPADA) {
                mensajes.addFlashAttribute("error", "No se puede modificar una celda ocupada.");
                return "redirect:/admin/celdas";
            }
            cargarListado(modelo, null, null);
            cargarFormulario(modelo, true, id, celda.getCodigo(), celda.getTipo().name());
            return "admin-celdas";
        } catch (DatosInvalidosException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/celdas";
        }
    }

    // CU-21: guardar la edición
    @PostMapping("/{id}/editar")
    public String guardarEdicion(@PathVariable Integer id,
                                 @RequestParam(required = false) String codigo,
                                 @RequestParam(required = false) TipoVehiculo tipo,
                                 Model modelo,
                                 RedirectAttributes mensajes) {
        try {
            gestionCeldaService.editar(id, codigo, tipo);
            mensajes.addFlashAttribute("exito", "Celda actualizada correctamente.");
            return "redirect:/admin/celdas";
        } catch (DatosInvalidosException e) {
            cargarListado(modelo, null, null);
            cargarFormulario(modelo, true, id, codigo, tipo == null ? "" : tipo.name());
            modelo.addAttribute("error", e.getMessage());
            return "admin-celdas";
        }
    }

    // CU-22: mantenimiento y habilitar
    @PostMapping("/{id}/mantenimiento")
    public String mantenimiento(@PathVariable Integer id, RedirectAttributes mensajes) {
        try {
            gestionCeldaService.ponerEnMantenimiento(id);
            mensajes.addFlashAttribute("exito", "Celda puesta en mantenimiento.");
        } catch (DatosInvalidosException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/celdas";
    }

    @PostMapping("/{id}/habilitar")
    public String habilitar(@PathVariable Integer id, RedirectAttributes mensajes) {
        try {
            gestionCeldaService.habilitar(id);
            mensajes.addFlashAttribute("exito", "Celda habilitada.");
        } catch (DatosInvalidosException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/celdas";
    }

    // ---------- Auxiliares ----------

    private void cargarListado(Model modelo, TipoVehiculo tipo, EstadoCelda estado) {
        modelo.addAttribute("celdas", gestionCeldaService.listar(tipo, estado));
        modelo.addAttribute("tipos", TipoVehiculo.values());
        modelo.addAttribute("estados", EstadoCelda.values());
        modelo.addAttribute("tipoFiltro", tipo == null ? "" : tipo.name());
        modelo.addAttribute("estadoFiltro", estado == null ? "" : estado.name());
    }

    private void cargarFormulario(Model modelo, boolean visible, Integer idEdicion,
                                  String codigo, String tipo) {
        modelo.addAttribute("mostrarForm", visible);
        modelo.addAttribute("idEdicion", idEdicion);
        modelo.addAttribute("codigoForm", codigo == null ? "" : codigo);
        modelo.addAttribute("tipoForm", tipo == null ? "" : tipo);
    }
}