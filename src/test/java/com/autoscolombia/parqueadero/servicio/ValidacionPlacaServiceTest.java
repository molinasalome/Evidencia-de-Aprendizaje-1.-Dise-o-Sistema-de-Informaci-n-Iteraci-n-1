package com.autoscolombia.parqueadero.servicio;

import com.autoscolombia.parqueadero.excepcion.PlacaInvalidaException;
import com.autoscolombia.parqueadero.modelo.TipoVehiculo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Formatos de placa: carros ABC123 y motos ABC12D (HU-13, CU-13)
class ValidacionPlacaServiceTest {

    private final ValidacionPlacaService servicio = new ValidacionPlacaService();

    @Test
    void placaDeCarroValidaSeReconoceComoCarro() {
        assertTrue(servicio.validarFormato("ABC123"));
        assertEquals(TipoVehiculo.CARRO, servicio.determinarTipo("ABC123"));
    }

    @Test
    void placaDeMotoValidaSeReconoceComoMoto() {
        assertTrue(servicio.validarFormato("ABC12D"));
        assertEquals(TipoVehiculo.MOTO, servicio.determinarTipo("ABC12D"));
    }

    @Test
    void laPlacaSeNormalizaAMayusculasYSinEspacios() {
        assertEquals("ABC123", servicio.normalizar("  abc123 "));
        assertEquals(TipoVehiculo.CARRO, servicio.determinarTipo(" abc123 "));
    }

    // Placas que parecen válidas pero no cumplen ninguno de los dos formatos
    @ParameterizedTest
    @ValueSource(strings = {"AB1234", "ABCD12", "ABC1234", "123ABC", "ABC12", "ABC-12", "ABC 12D", ""})
    void formatosInvalidosSeRechazan(String placa) {
        assertFalse(servicio.validarFormato(placa));
        assertThrows(PlacaInvalidaException.class, () -> servicio.determinarTipo(placa));
    }

    @Test
    void unaPlacaNulaEsInvalida() {
        assertFalse(servicio.validarFormato(null));
        assertThrows(PlacaInvalidaException.class, () -> servicio.determinarTipo(null));
    }
}