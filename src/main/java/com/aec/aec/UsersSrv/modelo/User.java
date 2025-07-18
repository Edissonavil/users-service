package com.aec.aec.UsersSrv.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor

public class User {
@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String nombreUsuario;

  @Column(nullable = false)
  private String clave;

  @Column(nullable = false)
  private String nombre;        // nombre

  @Column(nullable = false, unique = true)
  private String email;       // email

  @Enumerated(EnumType.STRING)
  private Rol rol;
    
}
