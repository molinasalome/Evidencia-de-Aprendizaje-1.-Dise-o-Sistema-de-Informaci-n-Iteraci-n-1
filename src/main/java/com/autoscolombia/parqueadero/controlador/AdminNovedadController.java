package com.autoscolombia.parqueadero.controlador;

import com.autoscolombia.parqueadero.modelo.Novedad;
import com.autoscolombia.parqueadero.modelo.TipoNovedad;
import com.autoscolombia.parqueadero.servicio.NovedadService;
import com.autoscolombia.parqueadero.servicio.ValidacionPlacaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Controller
public class AdminNovedadController {

    private static final int POR_PAGINA = 6;
    private static final DateTimeFormatter FORMATO =
        DateTimeFormatter.ofPattern("dd/MM/yyyy h:mm a", Locale.forLanguageTag("es-CO"));

    private final NovedadService novedadService;
    private final ValidacionPlacaService validacionPlacaService;

    public AdminNovedadController(NovedadService novedadService,
                                  ValidacionPlacaService validacionPlacaService) {
        this.novedadService = novedadService;
        this.validacionPlacaService = validacionPlacaService;
    }

    // Una fila de la tabla lista para mostrar
    public record FilaNovedad(String fechaHora, String placa, String tipo,
                              String descripcion, String registradaPor) {
    }

    @GetMapping("/admin/novedades")
    public String consultarNovedades(@RequestParam(required = false) String placa,
                                     @RequestParam(required = false) TipoNovedad tipo,
                                     @RequestParam(required = false)
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                     @RequestParam(required = false)
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                     @RequestParam(defaultValue = "1") int pagina,
                                     Model modelo) {

        List<FilaNovedad> todas;

        // Validaciones de los filtros
        String error = null;
        boolean hayPlaca = placa != null && !placa.isBlank();
        if (hayPlaca && !validacionPlacaService.validarFormato(placa)) {
            error = "Formato de placa inválido. Carros: 3 letras y 3 números. "
                  + "Motos: 3 letras, 2 números y 1 letra.";
        } else if (desde != null && hasta != null && hasta.isBefore(desde)) {
            error = "La fecha final no puede ser anterior a la fecha inicial.";
        }

        if (error != null) {
            modelo.addAttribute("error", error);
            todas = List.of();
        } else {
            todas = novedadService.consultarNovedades(placa, tipo, desde, hasta).stream()
                .map(this::aFila)
                .toList();
        }

       
        int totalPaginas = Math.max(1, (int) Math.ceil(todas.size() / (double) POR_PAGINA));
        int actual = Math.min(Math.max(pagina, 1), totalPaginas);
        int inicio = (actual - 1) * POR_PAGINA;
        List<FilaNovedad> visibles = todas.subList(inicio, Math.min(inicio + POR_PAGINA, todas.size()));

        modelo.addAttribute("filas", visibles);
        modelo.addAttribute("total", todas.size());
        modelo.addAttribute("paginaActual", actual);
        modelo.addAttribute("totalPaginas", totalPaginas);
        modelo.addAttribute("tipos", TipoNovedad.values());

        // Para que el formulario y los enlaces de página conserven los filtros
        modelo.addAttribute("placa", placa == null ? "" : placa.trim().toUpperCase());
        modelo.addAttribute("tipoSel", tipo == null ? "" : tipo.name());
        modelo.addAttribute("desde", desde == null ? "" : desde.toString());
        modelo.addAttribute("hasta", hasta == null ? "" : hasta.toString());
        return "admin-novedades";
    }

    private FilaNovedad aFila(Novedad n) {
        // Las novedades automáticas por falta de celda las genera el sistema
        String registradaPor = (n.getTipo() == TipoNovedad.SIN_CELDA_DISPONIBLE)
            ? "Sistema"
            : n.getEmpleado().getNombre();

        return new FilaNovedad(
            n.getFechaHora().format(FORMATO),
            n.getVehiculo().getPlaca(),
            n.getTipo().getEtiqueta(),
            n.getDescripcion(),
            registradaPor);
    }
}