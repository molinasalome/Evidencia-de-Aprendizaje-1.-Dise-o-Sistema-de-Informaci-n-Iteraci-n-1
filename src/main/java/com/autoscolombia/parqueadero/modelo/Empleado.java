package com.autoscolombia.parqueadero.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "empleado")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class Empleado extends Usuario {

    @Column(name = "nombre_usuario", nullable = false, length = 50, unique = true)
    private String nombreUsuario;

    @Column(name = "contrasena_hash", nullable = false, length = 255)
    private String contrasenaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private Rol rol;

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getContrasenaHash() { return contrasenaHash; }
    public void setContrasenaHash(String contrasenaHash) { this.contrasenaHash = contrasenaHash; }

    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
}