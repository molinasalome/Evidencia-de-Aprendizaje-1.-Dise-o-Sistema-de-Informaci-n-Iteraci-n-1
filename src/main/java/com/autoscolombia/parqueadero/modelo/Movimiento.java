package com.autoscolombia.parqueadero.modelo;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimiento")
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Integer idMovimiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "placa", nullable = false)
    private Vehiculo vehiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_celda")
    private Celda celda;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_empleado_entrada", nullable = false)
    private Empleado vigilanteEntrada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado_salida")
    private Empleado vigilanteSalida;

    @Column(name = "fecha_hora_entrada", nullable = false)
    private LocalDateTime fechaHoraEntrada;

    @Column(name = "fecha_hora_salida")
    private LocalDateTime fechaHoraSalida;

    // Minutos
    @Column(name = "tiempo_permanencia")
    private Integer tiempoPermanencia;

    // El movimiento está activo mientras no tenga fecha de salida
    public boolean estaActivo() {
        return fechaHoraSalida == null;
    }

    public Duration calcularPermanencia() {
        LocalDateTime fin = (fechaHoraSalida != null) ? fechaHoraSalida : LocalDateTime.now();
        return Duration.between(fechaHoraEntrada, fin);
    }

    public void registrarSalida(LocalDateTime fechaHora, Empleado vigilante) {
        this.fechaHoraSalida = fechaHora;
        this.vigilanteSalida = vigilante;
        this.tiempoPermanencia = (int) Duration.between(fechaHoraEntrada, fechaHora).toMinutes();
    }

    public Integer getIdMovimiento() { return idMovimiento; }
    public void setIdMovimiento(Integer idMovimiento) { this.idMovimiento = idMovimiento; }

    public Vehiculo getVehiculo() { return vehiculo; }
    public void setVehiculo(Vehiculo vehiculo) { this.vehiculo = vehiculo; }

    public Celda getCelda() { return celda; }
    public void setCelda(Celda celda) { this.celda = celda; }

    public Empleado getVigilanteEntrada() { return vigilanteEntrada; }
    public void setVigilanteEntrada(Empleado vigilanteEntrada) { this.vigilanteEntrada = vigilanteEntrada; }

    public Empleado getVigilanteSalida() { return vigilanteSalida; }
    public void setVigilanteSalida(Empleado vigilanteSalida) { this.vigilanteSalida = vigilanteSalida; }

    public LocalDateTime getFechaHoraEntrada() { return fechaHoraEntrada; }
    public void setFechaHoraEntrada(LocalDateTime fechaHoraEntrada) { this.fechaHoraEntrada = fechaHoraEntrada; }

    public LocalDateTime getFechaHoraSalida() { return fechaHoraSalida; }
    public void setFechaHoraSalida(LocalDateTime fechaHoraSalida) { this.fechaHoraSalida = fechaHoraSalida; }

    public Integer getTiempoPermanencia() { return tiempoPermanencia; }
    public void setTiempoPermanencia(Integer tiempoPermanencia) { this.tiempoPermanencia = tiempoPermanencia; }
}