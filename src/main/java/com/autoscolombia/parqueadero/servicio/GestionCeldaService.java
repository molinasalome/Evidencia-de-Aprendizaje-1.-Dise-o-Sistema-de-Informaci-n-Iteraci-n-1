package com.autoscolombia.parqueadero.servicio;

import com.autoscolombia.parqueadero.excepcion.DatosInvalidosException;
import com.autoscolombia.parqueadero.modelo.Celda;
import com.autoscolombia.parqueadero.modelo.EstadoCelda;
import com.autoscolombia.parqueadero.modelo.TipoVehiculo;
import com.autoscolombia.parqueadero.repositorio.CeldaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Administración de celdas (Iteración 2). La asignación en entradas y salidas
// sigue en CeldaService, de la Iteración 1.
@Service
@Transactional(readOnly = true)
public class GestionCeldaService {

    private static final int LARGO_MAXIMO_CODIGO = 10;

    private final CeldaRepository celdaRepository;

    public GestionCeldaService(CeldaRepository celdaRepository) {
        this.celdaRepository = celdaRepository;
    }

    // CU-23: listado con filtros (null = sin filtro)
    public List<Celda> listar(TipoVehiculo tipo, EstadoCelda estado) {
        return celdaRepository.filtrar(tipo, estado);
    }

    public Celda obtener(Integer idCelda) {
        return celdaRepository.findById(idCelda)
            .orElseThrow(() -> new DatosInvalidosException("La celda no existe."));
    }

    // CU-20: registrar una celda nueva (queda libre)
    @Transactional
    public void registrar(String codigo, TipoVehiculo tipo) {
        String cod = validarDatos(codigo, tipo);

        if (celdaRepository.existsByCodigo(cod)) {
            throw new DatosInvalidosException("Ya existe una celda con ese código.");
        }

        Celda celda = new Celda();
        celda.setCodigo(cod);
        celda.setTipo(tipo);
        celda.setEstado(EstadoCelda.LIBRE);
        celdaRepository.save(celda);
    }

    // CU-21: editar código y tipo, solo si la celda no está ocupada
    @Transactional
    public void editar(Integer idCelda, String codigo, TipoVehiculo tipo) {
        Celda celda = bloquear(idCelda);
        if (celda.getEstado() == EstadoCelda.OCUPADA) {
            throw new DatosInvalidosException("No se puede modificar una celda ocupada.");
        }

        String cod = validarDatos(codigo, tipo);
        if (celdaRepository.existsByCodigoAndIdCeldaNot(cod, idCelda)) {
            throw new DatosInvalidosException("Ya existe una celda con ese código.");
        }

        celda.setCodigo(cod);
        celda.setTipo(tipo);
        celdaRepository.save(celda);
    }

    // CU-22: pasar a mantenimiento (solo si no está ocupada)
    @Transactional
    public void ponerEnMantenimiento(Integer idCelda) {
        Celda celda = bloquear(idCelda);
        if (celda.getEstado() == EstadoCelda.OCUPADA) {
            throw new DatosInvalidosException(
                "No se puede poner en mantenimiento una celda ocupada.");
        }
        if (celda.getEstado() == EstadoCelda.MANTENIMIENTO) {
            throw new DatosInvalidosException("La celda ya está en mantenimiento.");
        }
        celda.setEstado(EstadoCelda.MANTENIMIENTO);
        celdaRepository.save(celda);
    }

    // CU-22: volver a habilitar una celda en mantenimiento
    @Transactional
    public void habilitar(Integer idCelda) {
        Celda celda = bloquear(idCelda);
        if (celda.getEstado() != EstadoCelda.MANTENIMIENTO) {
            throw new DatosInvalidosException("La celda no está en mantenimiento.");
        }
        celda.setEstado(EstadoCelda.LIBRE);
        celdaRepository.save(celda);
    }

    // ---------- Auxiliares ----------

    // Devuelve el código limpio y en mayúscula
    private String validarDatos(String codigo, TipoVehiculo tipo) {
        if (codigo == null || codigo.isBlank() || tipo == null) {
            throw new DatosInvalidosException("Completa el código y el tipo de la celda.");
        }
        String cod = codigo.trim().toUpperCase();
        if (cod.length() > LARGO_MAXIMO_CODIGO) {
            throw new DatosInvalidosException(
                "El código no puede tener más de " + LARGO_MAXIMO_CODIGO + " caracteres.");
        }
        return cod;
    }

    private Celda bloquear(Integer idCelda) {
        return celdaRepository.findByIdCelda(idCelda)
            .orElseThrow(() -> new DatosInvalidosException("La celda no existe."));
    }
}