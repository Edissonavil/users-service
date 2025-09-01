package com.aec.aec.UsersSrv.controller;

import com.aec.aec.UsersSrv.dto.ApiResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.aec.aec.UsersSrv.dto.ChangePasswordDto;
import com.aec.aec.UsersSrv.dto.ContactoDto;

import org.springframework.web.server.ResponseStatusException;

import com.aec.aec.UsersSrv.dto.RegisterClientDto;
import com.aec.aec.UsersSrv.dto.SolicitudCreadorDTO;
import com.aec.aec.UsersSrv.dto.UserDto;
import com.aec.aec.UsersSrv.dto.UserUpdateDto;
import com.aec.aec.UsersSrv.modelo.Rol;
import com.aec.aec.UsersSrv.modelo.User;
import com.aec.aec.UsersSrv.service.UserService;
import com.aec.aec.UsersSrv.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.RequestMapping;
import lombok.RequiredArgsConstructor;
import com.aec.aec.UsersSrv.service.EmailService;

// UserController.java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userSvc;
    private final EmailService emailService; // Asegúrate de que este servicio pueda enviar correos
    private final UserService userService;
    private final UserRepository userRepository;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserController.class);

    // 1) Registro público de clientes (ya existente)
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDto>> registerClient(
            @Valid @RequestBody RegisterClientDto dto) {
        UserDto toCreate = new UserDto();
        toCreate.setNombreUsuario(dto.getNombreUsuario());
        toCreate.setClave(dto.getClave());
        toCreate.setNombre(dto.getNombre());
        toCreate.setEmail(dto.getEmail());
        toCreate.setRol(Rol.ROL_CLIENTE);
        UserDto created = userSvc.register(toCreate);

        ApiResponse<UserDto> resp = new ApiResponse<>(
                "Cliente registrado correctamente",
                created);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resp);
    }

    // 2) Creación de Admin/Colaborador
    @PostMapping
    @PreAuthorize("hasAuthority('ROL_ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody UserDto dto) {

        // 1) Si es COLABORADOR y NO envían clave, generar una temporal
        String plainPasswordForEmail = dto.getClave();
        if (dto.getRol() == Rol.ROL_COLABORADOR && (plainPasswordForEmail == null || plainPasswordForEmail.isBlank())) {
            plainPasswordForEmail = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            dto.setClave(plainPasswordForEmail);
        }

        // 2) Registrar usuario (acá se encripta)
        UserDto created = userSvc.register(dto);

        // 3) Si es COLABORADOR, enviar email de bienvenida con credenciales (clave
        // temporal)
        boolean emailSent = false;
        String emailErrorMsg = null;
        if (dto.getRol() == Rol.ROL_COLABORADOR) {
            try {
                emailService.sendCollaboratorWelcomeEmail(
                        created.getEmail(), // usar el email resultante
                        created.getNombre(),
                        created.getNombreUsuario(),
                        plainPasswordForEmail);
                emailSent = true;
            } catch (Exception ex) {
                emailErrorMsg = ex.getMessage();
                // Log con stacktrace, no romper creación
                log.error("No se pudo enviar email de bienvenida a creador {} ({}): {}",
                        created.getNombreUsuario(), created.getEmail(), ex.getMessage(), ex);
            }
        }

        String baseMsg = switch (dto.getRol()) {
            case ROL_ADMIN -> "Administrador creado correctamente";
            case ROL_COLABORADOR -> "Creador creado correctamente";
            default -> "Usuario creado correctamente";
        };

        // Añadimos un sufijo informativo no sensible
        String finalMsg = baseMsg +
                (dto.getRol() == Rol.ROL_COLABORADOR
                        ? (emailSent ? " (correo de bienvenida enviado)"
                                : " (⚠ no se pudo enviar el correo de bienvenida)")
                        : "");

        ApiResponse<UserDto> resp = new ApiResponse<>(finalMsg, created);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    // 3) Obtener todos los usuarios (para AdminManageUsersPage)
    @GetMapping // GET /api/users
    @PreAuthorize("hasAuthority('ROL_ADMIN')") // hasAuthority es más granular para roles
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        List<UserDto> users = userSvc.findAllUsers(); // Necesitas implementar este método en UserService

        ApiResponse<List<UserDto>> resp = new ApiResponse<>(
                "Usuarios obtenidos correctamente",
                users);
        return ResponseEntity.ok(resp);
    }

    // 4) Obtener un usuario por ID (para edición en AdminManageUsersPage)
    @GetMapping("/{id}") // GET /api/users/{id}
    @PreAuthorize("hasAuthority('ROL_ADMIN') or #id == principal.id") // Admin puede ver cualquiera, usuario puede verse
                                                                      // a sí mismo
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        UserDto user = userSvc.findById(id); // Necesitas implementar este método en UserService
        return ResponseEntity.ok(user);
    }

    // 5) Actualizar usuario por ID (para AdminManageUsersPage)
    @PutMapping("/{id}") // PUT /api/users/{id}
    @PreAuthorize("hasAuthority('ROL_ADMIN') or #id == principal.id") // hasAuthority es correcto
    public ResponseEntity<UserDto> update(@PathVariable Long id,
            @Valid @RequestBody UserDto dto) {
        dto.setId(id);
        return ResponseEntity.ok(userSvc.updateMe(dto));
    }

    // 6) Eliminar usuario por ID (para AdminManageUsersPage)
    @DeleteMapping("/{id}") // DELETE /api/users/{id}
    @PreAuthorize("hasAuthority('ROL_ADMIN') or #id == principal.id") // hasAuthority es correcto
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userSvc.delete(id); // Necesitas implementar este método en UserService
        return ResponseEntity.noContent().build();
    }

    // 7) Resetear contraseña de usuario (para AdminManageUsersPage)
    @PostMapping("/{id}/reset-password") // POST /api/users/{id}/reset-password
    @PreAuthorize("hasAuthority('ROL_ADMIN')") // Solo un ADMIN puede resetear contraseñas
    public ResponseEntity<ApiResponse<Map<String, String>>> resetUserPassword(@PathVariable Long id) {
        String temporaryPassword = userSvc.resetPassword(id); // Implementar en UserService
        // Envía el correo con la contraseña temporal
        String userEmail = userSvc.findById(id).getEmail();
        emailService.sendTemporaryPasswordEmail(userEmail, temporaryPassword);

        ApiResponse<Map<String, String>> resp = new ApiResponse<>(
                "Contraseña reseteada. La contraseña temporal se ha generado.",
                Map.of("temporaryPassword", temporaryPassword) // Devuelve la contraseña temporal al admin
        );
        return ResponseEntity.ok(resp);
    }

    // 8) Endpoint para el perfil del usuario logueado (ya existente)
    @GetMapping("/me") // GET /api/users/me
    @PreAuthorize("hasAnyAuthority('ROL_CLIENTE','ROL_ADMIN','ROL_COLABORADOR')")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal UserDetails userDetails) {
        UserDto dto = userSvc.findByNombreUsuario(userDetails.getUsername());
        return ResponseEntity.ok(dto);
    }

    // 9) Solicitud de creador +
    @PostMapping("/solicitudes/creador")
    public ResponseEntity<?> crearSolicitud(@RequestBody SolicitudCreadorDTO dto) {
        emailService.sendCreatorApplicationEmail(dto.getNombreCompleto(), dto.getUsername(), dto.getEmail(),
                dto.getHablanosDeTi());
        return ResponseEntity.ok(Map.of("status", "enviado"));
    }

    // 10) Endpoint para cambiar la propia contraseña (para ProfileSettingsPage)
    @PutMapping("/me/change-password") // PUT /api/users/me/change-password
    @PreAuthorize("isAuthenticated()") // Solo requiere que el usuario esté autenticado
    public ResponseEntity<ApiResponse<String>> changeMyPassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ChangePasswordDto dto // Necesitas crear este DTO (currentPassword, newPassword)
    ) {
        userSvc.changePassword(userDetails.getUsername(), dto.getCurrentPassword(), dto.getNewPassword());
        ApiResponse<String> resp = new ApiResponse<>("Contraseña cambiada exitosamente", null);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserDto>> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserDto dto) {

        UserDto currentUserDto = userSvc.findByNombreUsuario(userDetails.getUsername());
        Long userId = currentUserDto.getId();

        dto.setId(userId);

        if (dto.getClave() != null && !dto.getClave().isBlank()) {
        }
        UserDto updated = userSvc.update(dto);
        ApiResponse<UserDto> resp = new ApiResponse<>(
                "Perfil actualizado correctamente",
                updated);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/request-password-reset")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(@RequestBody Map<String, String> body) {
        try {
            String username = body.get("username");
            userService.requestPasswordResetNotification(username);
            ApiResponse<Void> response = new ApiResponse<>(
                    "Solicitud de reseteo de contraseña procesada. Un administrador será notificado.", null);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            ApiResponse<Void> response = new ApiResponse<>(
                    "Solicitud de reseteo de contraseña procesada. Si el nombre de usuario es correcto, un administrador ha sido notificado.",
                    null);
            return ResponseEntity.ok(response); // No da pistas
        } catch (Exception e) {
            ApiResponse<Void> response = new ApiResponse<>(
                    "Error al procesar la solicitud de reseteo de contraseña: " + e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserDto> getUserByUsername(@PathVariable String username) {
        User u = userRepository.findByNombreUsuario(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        UserDto dto = UserDto.builder()
                .nombreUsuario(u.getNombreUsuario())
                .email(u.getEmail())
                .nombre(u.getNombre())
                .build();
        return ResponseEntity.ok(dto);
    }

    /** 1️⃣ ─── BUSCAR POR USERNAME (cadena) ─── */
    @GetMapping("/by-username/{username}")
    public ResponseEntity<UserDto> getByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.findByNombreUsuario(username));
    }

    /** 2️⃣ ─── BUSCAR POR ID NUMÉRICO ─── */
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<UserDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    /** 3️⃣ ─── SOlicitud de creadres Aec─── */
    @PostMapping("/solicitud-creador")
    public ResponseEntity<String> solicitarCreador(@RequestBody SolicitudCreadorDTO request) {
        try {
            emailService.sendCreatorApplicationEmail(
                    request.getNombreCompleto(),
                    request.getUsername(),
                    request.getEmail(),
                    request.getHablanosDeTi());
            return ResponseEntity.ok("Solicitud enviada con éxito.");
        } catch (MailException e) {
            return ResponseEntity.internalServerError().body("Error al enviar la solicitud: " + e.getMessage());
        } catch (IllegalStateException e) {
            // Manejo de errores de configuración (ej. adminEmailRecipient no configurado)
            return ResponseEntity.internalServerError().body(e.getMessage());
        } catch (Exception e) {
            // Otros errores inesperados
            return ResponseEntity.internalServerError().body("Ocurrió un error inesperado al procesar la solicitud.");
        }
    }

    /** 3️⃣ ─── SOlicitud de Contacto─── */

    @PostMapping("/contact")
    public ResponseEntity<String> enviarMensajeContacto(@RequestBody ContactoDto request) {
        try {
            emailService.sendContactEmail(
                    request.getNombre(),
                    request.getEmail(),
                    request.getAsunto(),
                    request.getMensaje());
            return ResponseEntity.ok("Mensaje enviado con éxito. Te contactaremos pronto.");
        } catch (MailException e) {
            // Manejo de errores de envío de correo
            return ResponseEntity.internalServerError().body("Error al enviar el mensaje: " + e.getMessage());
        } catch (IllegalStateException e) {
            // Manejo de errores de configuración
            return ResponseEntity.internalServerError().body(e.getMessage());
        } catch (Exception e) {
            // Otros errores inesperados
            return ResponseEntity.internalServerError().body("Ocurrió un error inesperado al procesar la solicitud.");
        }
    }

}