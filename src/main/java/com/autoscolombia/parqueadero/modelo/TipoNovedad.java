package com.autoscolombia.parqueadero.modelo;

public enum TipoNovedad {
    DANIO("Daño al vehículo"),
    OBJETO_OLVIDADO("Objeto olvidado"),
    INCIDENTE("Incidente de seguridad"),
    SIN_CELDA_DISPONIBLE("Sin celda disponible");

    private final String etiqueta;

    TipoNovedad(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}