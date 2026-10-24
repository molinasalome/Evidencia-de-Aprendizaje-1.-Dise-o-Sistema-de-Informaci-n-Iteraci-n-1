package com.autoscolombia.parqueadero.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "celda")
public class Celda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_celda")
    private Integer idCelda;

    @Column(name = "codigo", nullable = false, length = 10, unique = true)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoVehiculo tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoCelda estado = EstadoCelda.LIBRE;

    public void ocupar() {
        this.estado = EstadoCelda.OCUPADA;
    }

    public void liberar() {
        this.estado = EstadoCelda.LIBRE;
    }

    public boolean estaDisponible() {
        return this.estado == EstadoCelda.LIBRE;
    }

    public Integer getIdCelda() { return idCelda; }
    public void setIdCelda(Integer idCelda) { this.idCelda = idCelda; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public TipoVehiculo getTipo() { return tipo; }
    public void setTipo(TipoVehiculo tipo) { this.tipo = tipo; }

    public EstadoCelda getEstado() { return estado; }
    public void setEstado(EstadoCelda estado) { this.estado = estado; }
}