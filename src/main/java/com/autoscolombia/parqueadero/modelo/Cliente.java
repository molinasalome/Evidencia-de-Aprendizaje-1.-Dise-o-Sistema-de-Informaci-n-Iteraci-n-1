package com.autoscolombia.parqueadero.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "cliente")
@PrimaryKeyJoinColumn(name = "id_usuario")
public class Cliente extends Usuario {
}