package com.autoscolombia.parqueadero.excepcion;

public class PlacaInvalidaException extends RuntimeException {
    public PlacaInvalidaException(String mensaje) {
        super(mensaje);
    }
}