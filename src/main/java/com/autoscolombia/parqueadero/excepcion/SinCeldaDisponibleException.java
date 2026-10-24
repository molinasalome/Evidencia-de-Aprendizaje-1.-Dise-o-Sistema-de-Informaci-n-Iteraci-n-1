package com.autoscolombia.parqueadero.excepcion;

public class SinCeldaDisponibleException extends RuntimeException {
    public SinCeldaDisponibleException(String mensaje) {
        super(mensaje);
    }
}