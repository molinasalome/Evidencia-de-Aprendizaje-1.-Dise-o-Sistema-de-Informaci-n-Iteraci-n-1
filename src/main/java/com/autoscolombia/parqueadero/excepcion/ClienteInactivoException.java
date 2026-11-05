package com.autoscolombia.parqueadero.excepcion;

public class ClienteInactivoException extends RuntimeException {
    public ClienteInactivoException(String mensaje) {
        super(mensaje);
    }
}