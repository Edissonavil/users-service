package com.aec.aec.UsersSrv.dto;
import com.aec.aec.UsersSrv.modelo.Rol;



import lombok.*;
import jakarta.validation.constraints.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder  

public class UserDto {
    private Long id;

    @NotBlank(message = "El nombre de usuario no debe estar vacío")
    @Size(min = 4, max = 20, message = "El nombre de usuario debe tener entre 4 y 20 caracteres")
    private String nombreUsuario;

    private String clave;

    @NotBlank(message = "El nombre no debe estar vacío")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
    private String nombre;

    @NotBlank(message = "El email no debe estar vacío")
    @Email(message = "Formato de email inválido")
    @Size(max = 100, message = "El email no puede exceder los 100 caracteres")
    private String email;

    @NotNull(message = "El rol no debe ser nulo")
    private Rol rol;

    /**  
     * Ahora se valida **solo** cuando usemos el grupo UpdateGroup  
     * En el flujo de creación NO pertenece al grupo Default.
     */

    // getters y setters...
}