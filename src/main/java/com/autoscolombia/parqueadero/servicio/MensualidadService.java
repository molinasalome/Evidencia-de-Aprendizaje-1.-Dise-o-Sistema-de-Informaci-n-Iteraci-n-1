package com.autoscolombia.parqueadero.servicio;

import com.autoscolombia.parqueadero.excepcion.DatosInvalidosException;
import com.autoscolombia.parqueadero.modelo.Cliente;
import com.autoscolombia.parqueadero.modelo.Mensualidad;
import com.autoscolombia.parqueadero.repositorio.ClienteRepository;
import com.autoscolombia.parqueadero.repositorio.MensualidadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MensualidadService {

    // Se puede renovar cuando faltan 5 días o menos para el vencimiento (HU-15)
    private static final int DIAS_PARA_RENOVAR = 5;

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Qué debe mostrar la pantalla para este cliente:
    // situacion = SIN_MENSUALIDAD (se puede registrar), RENOVABLE (se puede renovar)
    //             o VIGENTE (todavía no se puede renovar)
    public record InfoMensualidad(String situacion, LocalDate inicioRenovacion,
                                  LocalDate vigenteHasta, LocalDate renovableDesde) {
    }

    // Una fila del historial de mensualidades
    public record FilaMensualidad(String inicio, String fin, String estado) {
    }

    private final MensualidadRepository mensualidadRepository;
    private final ClienteRepository clienteRepository;

    public MensualidadService(MensualidadRepository mensualidadRepository,
                              ClienteRepository clienteRepository) {
        this.mensualidadRepository = mensualidadRepository;
        this.clienteRepository = clienteRepository;
    }

    // CU-14: registrar la mensualidad de un cliente
    @Transactional
    public void registrar(Integer idCliente, LocalDate inicio, LocalDate fin) {
        Cliente cliente = obtenerCliente(idCliente);
        validarFechas(inicio, fin);

        // Un cliente solo puede tener una mensualidad vigente a la vez
        if (!mensualidadRepository.buscarVigentes(idCliente, LocalDate.now()).isEmpty()) {
            throw new DatosInvalidosException(
                "El cliente ya tiene una mensualidad vigente. "
                + "Puedes renovarla cuando le falten " + DIAS_PARA_RENOVAR + " días o menos.");
        }
        guardar(cliente, inicio, fin);
    }

    // CU-15: renovar. La fecha de inicio la calcula el sistema; solo se elige la fecha fin.
    @Transactional
    public void renovar(Integer idCliente, LocalDate fin) {
        Cliente cliente = obtenerCliente(idCliente);
        InfoMensualidad info = consultar(idCliente);

        if (info.situacion().equals("SIN_MENSUALIDAD")) {
            throw new DatosInvalidosException(
                "El cliente no tiene mensualidad para renovar. Regístrala primero.");
        }
        if (info.situacion().equals("VIGENTE")) {
            throw new DatosInvalidosException(
                "La mensualidad vigente vence el " + info.vigenteHasta().format(FORMATO)
                + ". Podrás renovarla desde el " + info.renovableDesde().format(FORMATO) + ".");
        }

        validarFechas(info.inicioRenovacion(), fin);
        // La mensualidad anterior no se toca: se conserva en el historial
        guardar(cliente, info.inicioRenovacion(), fin);
    }

    // Calcula en qué situación está el cliente y desde cuándo empezaría una renovación
    public InfoMensualidad consultar(Integer idCliente) {
        LocalDate hoy = LocalDate.now();

        Mensualidad ultima = mensualidadRepository
            .findFirstByClienteIdUsuarioOrderByFechaFinDesc(idCliente)
            .orElse(null);

        if (ultima == null) {
            return new InfoMensualidad("SIN_MENSUALIDAD", null, null, null);
        }

        LocalDate fechaFin = ultima.getFechaFin();

        // Vencida: la nueva empieza hoy
        if (fechaFin.isBefore(hoy)) {
            return new InfoMensualidad("RENOVABLE", hoy, null, null);
        }

        // Vigente y por vencer: la nueva empieza el día siguiente al fin de la anterior
        long diasRestantes = ChronoUnit.DAYS.between(hoy, fechaFin);
        if (diasRestantes <= DIAS_PARA_RENOVAR) {
            return new InfoMensualidad("RENOVABLE", fechaFin.plusDays(1), null, null);
        }

        // Vigente con más de 5 días: todavía no se puede renovar
        return new InfoMensualidad("VIGENTE", null, fechaFin,
                                   fechaFin.minusDays(DIAS_PARA_RENOVAR));
    }

    // Historial del cliente, la más reciente primero
    public List<FilaMensualidad> historial(Integer idCliente) {
        LocalDate hoy = LocalDate.now();
        return mensualidadRepository.findByClienteIdUsuarioOrderByFechaInicioDesc(idCliente)
            .stream()
            .map(m -> new FilaMensualidad(
                m.getFechaInicio().format(FORMATO),
                m.getFechaFin().format(FORMATO),
                estadoDe(m, hoy)))
            .toList();
    }

    // ---------- Auxiliares ----------

    private String estadoDe(Mensualidad m, LocalDate hoy) {
        if (m.estaVigente(hoy)) {
            return "Vigente";
        }
        return m.getFechaInicio().isAfter(hoy) ? "Programada" : "Finalizada";
    }

    private void guardar(Cliente cliente, LocalDate inicio, LocalDate fin) {
        Mensualidad m = new Mensualidad();
        m.setCliente(cliente);
        m.setFechaInicio(inicio);
        m.setFechaFin(fin);
        mensualidadRepository.save(m);
    }

    private void validarFechas(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            throw new DatosInvalidosException("Indica la fecha de inicio y la fecha de fin.");
        }
        if (fin.isBefore(inicio)) {
            throw new DatosInvalidosException(
                "La fecha final no puede ser anterior a la fecha inicial.");
        }
    }

    private Cliente obtenerCliente(Integer idCliente) {
        return clienteRepository.findById(idCliente)
            .orElseThrow(() -> new DatosInvalidosException("El cliente no existe."));
    }
}