package com.autoscolombia.parqueadero.controlador;

import com.autoscolombia.parqueadero.modelo.Movimiento;
import com.autoscolombia.parqueadero.servicio.MovimientoService;
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
public class HistorialController {

    private static final int POR_PAGINA = 6;
    private static final DateTimeFormatter FORMATO =
        DateTimeFormatter.ofPattern("dd/MM/yyyy h:mm a", Locale.forLanguageTag("es-CO"));

    private final MovimientoService movimientoService;
    private final ValidacionPlacaService validacionPlacaService;

    public HistorialController(MovimientoService movimientoService,
                               ValidacionPlacaService validacionPlacaService) {
        this.movimientoService = movimientoService;
        this.validacionPlacaService = validacionPlacaService;
    }

    // Una fila del historial lista para mostrar
    public record FilaHistorial(String placa, String usuario, String celda, String entrada,
                                String salida, String permanencia, boolean dentro) {
    }

    @GetMapping("/admin/historial")
    public String historial(@RequestParam(required = false) String placa,
                            @RequestParam(required = false) String usuario,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                            @RequestParam(required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                            @RequestParam(defaultValue = "1") int pagina,
                            Model modelo) {

        List<FilaHistorial> todas;

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
            todas = movimientoService.consultarHistorial(placa, usuario, desde, hasta).stream()
                .map(this::aFila)
                .toList();
        }

        // Paginación de 6 en 6
        int totalPaginas = Math.max(1, (int) Math.ceil(todas.size() / (double) POR_PAGINA));
        int actual = Math.min(Math.max(pagina, 1), totalPaginas);
        int inicio = (actual - 1) * POR_PAGINA;
        List<FilaHistorial> visibles = todas.subList(inicio, Math.min(inicio + POR_PAGINA, todas.size()));

        modelo.addAttribute("filas", visibles);
        modelo.addAttribute("total", todas.size());
        modelo.addAttribute("paginaActual", actual);
        modelo.addAttribute("totalPaginas", totalPaginas);

        // Para que el formulario y los enlaces de página conserven los filtros
        modelo.addAttribute("placa", placa == null ? "" : placa.trim().toUpperCase());
        modelo.addAttribute("usuario", usuario == null ? "" : usuario.trim());
        modelo.addAttribute("desde", desde == null ? "" : desde.toString());
        modelo.addAttribute("hasta", hasta == null ? "" : hasta.toString());
        return "admin-historial";
    }

    private FilaHistorial aFila(Movimiento m) {
        boolean dentro = m.getFechaHoraSalida() == null;
        return new FilaHistorial(
            m.getVehiculo().getPlaca(),
            m.getVehiculo().getCliente().getNombre(),
            m.getCelda() != null ? m.getCelda().getCodigo() : "Sin celda",
            m.getFechaHoraEntrada().format(FORMATO),
            dentro ? "--" : m.getFechaHoraSalida().format(FORMATO),
            dentro ? "En curso" : textoMinutos(m.getTiempoPermanencia()),
            dentro);
    }

    private String textoMinutos(Integer minutos) {
        if (minutos == null) {
            return "-";
        }
        int horas = minutos / 60;
        int resto = minutos % 60;
        return horas > 0 ? horas + " h " + resto + " min" : resto + " min";
    }
}