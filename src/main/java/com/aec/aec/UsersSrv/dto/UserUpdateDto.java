// src/main/java/com/aec/aec/UsersSrv/dto/UserUpdateDto.java
package com.aec.aec.UsersSrv.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserUpdateDto {
    @NotBlank
    private String nombre;
    @NotBlank
    private String email;
    @NotBlank
    private String currentPassword;
}

