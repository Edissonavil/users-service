package com.aec.aec.UsersSrv.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

// Usando Lombok para simplificar getters/setters/constructores
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudCreadorDTO {
    private String nombreCompleto;
    private String username;
    private String email;
}