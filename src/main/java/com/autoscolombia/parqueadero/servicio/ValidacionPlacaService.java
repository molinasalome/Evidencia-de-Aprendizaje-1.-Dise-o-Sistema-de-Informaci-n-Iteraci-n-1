package com.autoscolombia.parqueadero.servicio;

import com.autoscolombia.parqueadero.excepcion.PlacaInvalidaException;
import com.autoscolombia.parqueadero.modelo.TipoVehiculo;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class ValidacionPlacaService {

    // Carros: 3 letras y 3 números (ABC123)
    private static final Pattern PLACA_CARRO = Pattern.compile("^[A-Z]{3}[0-9]{3}$");

    // Motos: 3 letras, 2 números y 1 letra (ABC12D)
    private static final Pattern PLACA_MOTO = Pattern.compile("^[A-Z]{3}[0-9]{2}[A-Z]$");

    // Quita espacios y pasa a mayúsculas para comparar siempre igual
    public String normalizar(String placa) {
        return placa == null ? "" : placa.trim().toUpperCase();
    }

    public boolean validarFormato(String placa) {
        String p = normalizar(placa);
        return PLACA_CARRO.matcher(p).matches() || PLACA_MOTO.matcher(p).matches();
    }

    public TipoVehiculo determinarTipo(String placa) {
        String p = normalizar(placa);
        if (PLACA_CARRO.matcher(p).matches()) {
            return TipoVehiculo.CARRO;
        }
        if (PLACA_MOTO.matcher(p).matches()) {
            return TipoVehiculo.MOTO;
        }
        throw new PlacaInvalidaException(
            "Formato de placa inválido. Carros: 3 letras y 3 números. Motos: 3 letras, 2 números y 1 letra.");
    }
}