package com.autoscolombia.parqueadero.servicio;

import com.autoscolombia.parqueadero.excepcion.DatosInvalidosException;
import com.autoscolombia.parqueadero.modelo.Cliente;
import com.autoscolombia.parqueadero.modelo.Mensualidad;
import com.autoscolombia.parqueadero.repositorio.ClienteRepository;
import com.autoscolombia.parqueadero.repositorio.MensualidadRepository;
import com.autoscolombia.parqueadero.servicio.MensualidadService.InfoMensualidad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Reglas de registrar y renovar mensualidades (HU-14 y HU-15, CU-14 y CU-15)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MensualidadServiceTest {

    private static final Integer ID_CLIENTE = 1;

    @Mock
    private MensualidadRepository mensualidadRepository;
    @Mock
    private ClienteRepository clienteRepository;

    private MensualidadService servicio;
    private final LocalDate hoy = LocalDate.now();

    @BeforeEach
    void preparar() {
        servicio = new MensualidadService(mensualidadRepository, clienteRepository);
        when(clienteRepository.findById(ID_CLIENTE)).thenReturn(Optional.of(new Cliente()));
    }

    // Deja como última mensualidad del cliente una que termina en la fecha indicada
    private void ultimaMensualidadTerminaEn(LocalDate fin) {
        Mensualidad m = new Mensualidad();
        m.setFechaInicio(fin.minusDays(30));
        m.setFechaFin(fin);
        when(mensualidadRepository.findFirstByClienteIdUsuarioOrderByFechaFinDesc(ID_CLIENTE))
            .thenReturn(Optional.of(m));
    }

    // ---------- Situación del cliente ----------

    @Test
    void clienteSinMensualidadPuedeRegistrar() {
        assertEquals("SIN_MENSUALIDAD", servicio.consultar(ID_CLIENTE).situacion());
    }

    @Test
    void conCincoDiasRestantesSePuedeRenovar() {
        ultimaMensualidadTerminaEn(hoy.plusDays(5));

        InfoMensualidad info = servicio.consultar(ID_CLIENTE);

        assertEquals("RENOVABLE", info.situacion());
        assertEquals(hoy.plusDays(6), info.inicioRenovacion());
    }

    @Test
    void conSeisDiasRestantesTodaviaNoSePuedeRenovar() {
        ultimaMensualidadTerminaEn(hoy.plusDays(6));

        InfoMensualidad info = servicio.consultar(ID_CLIENTE);

        assertEquals("VIGENTE", info.situacion());
        assertEquals(hoy.plusDays(6), info.vigenteHasta());
        assertEquals(hoy.plusDays(1), info.renovableDesde());
    }

    @Test
    void siVenceHoyLaNuevaEmpiezaManana() {
        ultimaMensualidadTerminaEn(hoy);

        InfoMensualidad info = servicio.consultar(ID_CLIENTE);

        assertEquals("RENOVABLE", info.situacion());
        assertEquals(hoy.plusDays(1), info.inicioRenovacion());
    }

    @Test
    void siYaVencioLaNuevaEmpiezaHoy() {
        ultimaMensualidadTerminaEn(hoy.minusDays(1));

        InfoMensualidad info = servicio.consultar(ID_CLIENTE);

        assertEquals("RENOVABLE", info.situacion());
        assertEquals(hoy, info.inicioRenovacion());
    }

    // ---------- Renovar ----------

    @Test
    void renovarConCincoDiasGuardaLaNuevaDesdeElDiaSiguiente() {
        ultimaMensualidadTerminaEn(hoy.plusDays(5));
        LocalDate nuevoFin = hoy.plusDays(36);

        servicio.renovar(ID_CLIENTE, nuevoFin);

        ArgumentCaptor<Mensualidad> captor = ArgumentCaptor.forClass(Mensualidad.class);
        verify(mensualidadRepository).save(captor.capture());
        assertEquals(hoy.plusDays(6), captor.getValue().getFechaInicio());
        assertEquals(nuevoFin, captor.getValue().getFechaFin());
    }

    @Test
    void renovarConSeisDiasSeRechazaYNoGuardaNada() {
        ultimaMensualidadTerminaEn(hoy.plusDays(6));

        assertThrows(DatosInvalidosException.class,
            () -> servicio.renovar(ID_CLIENTE, hoy.plusDays(40)));
        verify(mensualidadRepository, never()).save(any());
    }

    @Test
    void renovarSinMensualidadPreviaSeRechaza() {
        assertThrows(DatosInvalidosException.class,
            () -> servicio.renovar(ID_CLIENTE, hoy.plusDays(30)));
        verify(mensualidadRepository, never()).save(any());
    }

    @Test
    void renovarConFinAnteriorAlInicioSeRechaza() {
        ultimaMensualidadTerminaEn(hoy.plusDays(5));   // la nueva empezaría en hoy + 6

        assertThrows(DatosInvalidosException.class,
            () -> servicio.renovar(ID_CLIENTE, hoy.plusDays(3)));
        verify(mensualidadRepository, never()).save(any());
    }

    // ---------- Registrar ----------

    @Test
    void registrarMensualidadValidaLaGuarda() {
        servicio.registrar(ID_CLIENTE, hoy, hoy.plusDays(30));

        verify(mensualidadRepository).save(any(Mensualidad.class));
    }

    @Test
    void noSePuedeRegistrarSiYaHayUnaVigente() {
        when(mensualidadRepository.buscarVigentes(eq(ID_CLIENTE), any(LocalDate.class)))
            .thenReturn(List.of(new Mensualidad()));

        assertThrows(DatosInvalidosException.class,
            () -> servicio.registrar(ID_CLIENTE, hoy, hoy.plusDays(30)));
        verify(mensualidadRepository, never()).save(any());
    }

    @Test
    void registrarConFinAnteriorAlInicioSeRechaza() {
        assertThrows(DatosInvalidosException.class,
            () -> servicio.registrar(ID_CLIENTE, hoy, hoy.minusDays(1)));
        verify(mensualidadRepository, never()).save(any());
    }
}