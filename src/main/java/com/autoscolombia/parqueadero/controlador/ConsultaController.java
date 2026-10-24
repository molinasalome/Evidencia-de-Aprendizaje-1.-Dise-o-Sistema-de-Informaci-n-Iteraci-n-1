package com.autoscolombia.parqueadero.controlador;

import com.autoscolombia.parqueadero.modelo.TipoVehiculo;
import com.autoscolombia.parqueadero.servicio.CeldaService;
import com.autoscolombia.parqueadero.servicio.MovimientoService;
import com.autoscolombia.parqueadero.servicio.MovimientoService.VehiculoDentro;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ConsultaController {

    private static final int POR_PAGINA = 6;

    private final MovimientoService movimientoService;
    private final CeldaService celdaService;

    public ConsultaController(MovimientoService movimientoService, CeldaService celdaService) {
        this.movimientoService = movimientoService;
        this.celdaService = celdaService;
    }

    @GetMapping("/consultas/dentro")
    public String vehiculosDentro(@RequestParam(required = false) String placa,
                                  @RequestParam(required = false) TipoVehiculo tipo,
                                  @RequestParam(defaultValue = "1") int pagina,
                                  Model modelo) {
        List<VehiculoDentro> todos = movimientoService.consultarVehiculosDentroDetalle();

        // Filtros: la placa busca coincidencias parciales
        String filtro = (placa == null) ? "" : placa.trim().toUpperCase();
        List<VehiculoDentro> filtrados = todos.stream()
            .filter(v -> filtro.isEmpty() || v.placa().contains(filtro))
            .filter(v -> tipo == null || v.tipo() == tipo)
            .toList();

        // Paginación de 6 en 6
        int totalPaginas = Math.max(1, (int) Math.ceil(filtrados.size() / (double) POR_PAGINA));
        int actual = Math.min(Math.max(pagina, 1), totalPaginas);
        int desde = (actual - 1) * POR_PAGINA;
        List<VehiculoDentro> visibles =
            filtrados.subList(desde, Math.min(desde + POR_PAGINA, filtrados.size()));

        modelo.addAttribute("visibles", visibles);
        modelo.addAttribute("totalDentro", todos.size());
        modelo.addAttribute("totalFiltrados", filtrados.size());
        modelo.addAttribute("paginaActual", actual);
        modelo.addAttribute("totalPaginas", totalPaginas);
        modelo.addAttribute("placaFiltro", filtro);
        modelo.addAttribute("tipoSel", tipo == null ? "" : tipo.name());
        return "consulta-dentro";
    }

    @GetMapping("/consultas/celdas")
    public String disponibilidadCeldas(Model modelo) {
        modelo.addAttribute("m", celdaService.obtenerMapa());
        return "consulta-celdas";
    }
}