package com.autoscolombia.parqueadero.controlador;

import com.autoscolombia.parqueadero.excepcion.DatosInvalidosException;
import com.autoscolombia.parqueadero.modelo.Rol;
import com.autoscolombia.parqueadero.servicio.EmpleadoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Gestión de empleados: solo el administrador (la ruta /admin/** ya está protegida)
@Controller
@RequestMapping("/admin/empleados")
public class EmpleadoController {

    private final EmpleadoService empleadoService;

    public EmpleadoController(EmpleadoService empleadoService) {
        this.empleadoService = empleadoService;
    }

    @GetMapping
    public String listar(Model modelo) {
        cargarListado(modelo);
        modelo.addAttribute("mostrarForm", false);
        modelo.addAttribute("nombre", "");
        modelo.addAttribute("documento", "");
        modelo.addAttribute("usuario", "");
        modelo.addAttribute("rolSel", "");
        return "admin-empleados";
    }

    @PostMapping
    public String registrar(@RequestParam(required = false) String nombre,
                            @RequestParam(required = false) String documento,
                            @RequestParam(required = false) String usuario,
                            @RequestParam(required = false) Rol rol,
                            @RequestParam(required = false) String contrasena,
                            Model modelo,
                            RedirectAttributes mensajes) {
        try {
            empleadoService.registrar(nombre, documento, usuario, rol, contrasena);
            mensajes.addFlashAttribute("exito", "Empleado registrado correctamente.");
            return "redirect:/admin/empleados";
        } catch (DatosInvalidosException e) {
            // Se vuelve a mostrar el formulario con lo escrito (menos la contraseña)
            cargarListado(modelo);
            modelo.addAttribute("error", e.getMessage());
            modelo.addAttribute("mostrarForm", true);
            modelo.addAttribute("nombre", nombre == null ? "" : nombre);
            modelo.addAttribute("documento", documento == null ? "" : documento);
            modelo.addAttribute("usuario", usuario == null ? "" : usuario);
            modelo.addAttribute("rolSel", rol == null ? "" : rol.name());
            return "admin-empleados";
        }
    }

    private void cargarListado(Model modelo) {
        modelo.addAttribute("empleados", empleadoService.listar());
        modelo.addAttribute("roles", Rol.values());
    }
}