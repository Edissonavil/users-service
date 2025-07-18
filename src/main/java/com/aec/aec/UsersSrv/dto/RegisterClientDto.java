package com.aec.aec.UsersSrv.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterClientDto {
    @NotBlank
    private String nombreUsuario;

    @NotBlank
    private String clave;

    @NotBlank
    private String nombre;

    @Email @NotBlank
    private String email;
}

