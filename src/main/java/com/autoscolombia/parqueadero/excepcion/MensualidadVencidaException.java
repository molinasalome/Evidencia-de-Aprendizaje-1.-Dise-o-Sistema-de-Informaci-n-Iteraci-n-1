package com.autoscolombia.parqueadero.excepcion;

public class MensualidadVencidaException extends RuntimeException {
    public MensualidadVencidaException(String mensaje) {
        super(mensaje);
    }
}