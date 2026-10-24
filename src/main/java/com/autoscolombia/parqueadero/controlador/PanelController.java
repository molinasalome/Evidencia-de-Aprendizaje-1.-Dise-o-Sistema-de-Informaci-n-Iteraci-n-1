package com.autoscolombia.parqueadero.controlador;

import com.autoscolombia.parqueadero.servicio.CeldaService;
import com.autoscolombia.parqueadero.servicio.MovimientoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PanelController {

    private final CeldaService celdaService;
    private final MovimientoService movimientoService;

    public PanelController(CeldaService celdaService, MovimientoService movimientoService) {
        this.celdaService = celdaService;
        this.movimientoService = movimientoService;
    }

    @GetMapping("/panel")
    public String panel(Model modelo) {
        modelo.addAttribute("d", celdaService.obtenerDisponibilidad());
        modelo.addAttribute("dentro", movimientoService.consultarVehiculosDentro().size());
        return "panel";
    }
}