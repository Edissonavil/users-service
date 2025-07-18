package com.aec.aec.UsersSrv.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class UsernameRequest {
    @NotBlank(message = "El nombre de usuario no puede estar vacío")
    private String username;
}
