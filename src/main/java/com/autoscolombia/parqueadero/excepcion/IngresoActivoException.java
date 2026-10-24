package com.autoscolombia.parqueadero.excepcion;

public class IngresoActivoException extends RuntimeException {
    public IngresoActivoException(String mensaje) {
        super(mensaje);
    }
}