package com.autoscolombia.parqueadero.controlador;

import com.autoscolombia.parqueadero.excepcion.PlacaInvalidaException;
import com.autoscolombia.parqueadero.modelo.TipoNovedad;
import com.autoscolombia.parqueadero.servicio.NovedadService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Controller
public class NovedadController {

    private final NovedadService novedadService;

    public NovedadController(NovedadService novedadService) {
        this.novedadService = novedadService;
    }

    @GetMapping("/novedades/registrar")
    public String formularioNovedad(Authentication autenticacion, Model modelo) {
        // El vigilante no puede escoger SIN_CELDA_DISPONIBLE: esa la genera el sistema (RF-09)
        Map<TipoNovedad, String> tipos = new LinkedHashMap<>();
        tipos.put(TipoNovedad.DANIO, "Daño");
        tipos.put(TipoNovedad.OBJETO_OLVIDADO, "Objeto olvidado");
        tipos.put(TipoNovedad.INCIDENTE, "Incidente");
        modelo.addAttribute("tipos", tipos);

        // Campos de solo lectura del mockup
        modelo.addAttribute("fechaHora", LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a", Locale.forLanguageTag("es-CO"))));
        modelo.addAttribute("registradaPor", autenticacion.getName());
        return "registro-novedades";
    }

    @PostMapping("/novedades/registrar")
    public String registrarNovedad(@RequestParam String placa,
                                   @RequestParam TipoNovedad tipo,
                                   @RequestParam String descripcion,
                                   Authentication autenticacion,
                                   RedirectAttributes mensajes) {
        try {
            novedadService.registrarNovedad(placa, tipo, descripcion, autenticacion.getName());
            mensajes.addFlashAttribute("exito", "Novedad registrada correctamente.");
        } catch (PlacaInvalidaException | IllegalStateException | IllegalArgumentException e) {
            // Placa inválida, vehículo sin ingreso activo o datos incompletos
            mensajes.addFlashAttribute("error", e.getMessage());
            // Se devuelve lo escrito para que el vigilante no lo pierda
            mensajes.addFlashAttribute("placaEscrita", placa);
            mensajes.addFlashAttribute("tipoEscrito", tipo);
            mensajes.addFlashAttribute("descripcionEscrita", descripcion);
        }
        return "redirect:/novedades/registrar";
    }
}