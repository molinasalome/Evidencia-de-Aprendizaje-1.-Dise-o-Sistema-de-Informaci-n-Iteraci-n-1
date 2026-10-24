package com.autoscolombia.parqueadero.controlador;

import com.autoscolombia.parqueadero.excepcion.IngresoActivoException;
import com.autoscolombia.parqueadero.excepcion.MensualidadVencidaException;
import com.autoscolombia.parqueadero.excepcion.PlacaInvalidaException;
import com.autoscolombia.parqueadero.modelo.Movimiento;
import com.autoscolombia.parqueadero.servicio.MovimientoService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MovimientoController {

    private final MovimientoService movimientoService;

    public MovimientoController(MovimientoService movimientoService) {
        this.movimientoService = movimientoService;
    }

    @GetMapping("/")
    public String inicio() {
        return "redirect:/panel";
    }

    // ---------- Registro de Entrada ----------

    @GetMapping("/entrada")
    public String formularioEntrada() {
        return "registro-entrada";
    }

    @PostMapping("/entrada")
    public String registrarEntrada(@RequestParam String placa,
                                   Authentication autenticacion,
                                   RedirectAttributes mensajes) {
        try {
            Movimiento movimiento = movimientoService.registrarEntrada(placa, autenticacion.getName());

            if (movimiento.getCelda() != null) {
                mensajes.addFlashAttribute("exito",
                    "Entrada registrada. Celda asignada: " + movimiento.getCelda().getCodigo());
            } else {
                mensajes.addFlashAttribute("alerta",
                    "Entrada registrada SIN celda: no hay celda libre. "
                    + "Se registró una novedad para el administrador.");
            }
        } catch (PlacaInvalidaException | MensualidadVencidaException | IngresoActivoException e) {
            mensajes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/entrada";
    }

    // ---------- Registro de Salida ----------

    @GetMapping("/salida")
    public String formularioSalida() {
        return "registro-salida";
    }

    @PostMapping("/salida")
    public String registrarSalida(@RequestParam String placa,
                                  Authentication autenticacion,
                                  RedirectAttributes mensajes) {
        try {
            Movimiento movimiento = movimientoService.registrarSalida(placa, autenticacion.getName());

            String celda = (movimiento.getCelda() != null)
                ? " Celda liberada: " + movimiento.getCelda().getCodigo() + "."
                : "";
            mensajes.addFlashAttribute("exito",
                "Salida registrada. Tiempo de permanencia: "
                + movimiento.getTiempoPermanencia() + " minutos." + celda);
        } catch (PlacaInvalidaException | IllegalStateException e) {
            // No existe una entrada activa para la placa (RF-12)
            mensajes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/salida";
    }
}