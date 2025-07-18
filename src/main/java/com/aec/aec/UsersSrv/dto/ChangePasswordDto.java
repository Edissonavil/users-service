package com.aec.aec.UsersSrv.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordDto {
    @NotBlank(message = "La contraseña actual no puede estar vacía")
    private String currentPassword;

    @NotBlank(message = "La nueva contraseña no puede estar vacía")
    private String newPassword;
}