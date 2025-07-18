package com.aec.aec.UsersSrv.service;

import com.aec.aec.UsersSrv.dto.UserDto;
import com.aec.aec.UsersSrv.dto.ChangePasswordDto;
import com.aec.aec.UsersSrv.modelo.Rol;
import com.aec.aec.UsersSrv.modelo.User;
import com.aec.aec.UsersSrv.repository.UserRepository;
import com.aec.aec.UsersSrv.exception.DuplicateResourceException;
import com.aec.aec.UsersSrv.exception.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Value; 
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${admin.email}") // ¡Declaración e inyección de adminEmail desde application.properties!
    private String adminEmail;

    @Transactional
    public UserDto register(UserDto dto) {
        // 1) Validar unicidad email
        if (userRepo.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Email ya registrado: " + dto.getEmail());
        }
        // 2) Validar unicidad nombreUsuario
        if (userRepo.existsByNombreUsuario(dto.getNombreUsuario())) {
            throw new DuplicateResourceException("Nombre de usuario ya usado: " + dto.getNombreUsuario());
        }
        // 3) DTO → Entidad (encriptando la clave una sola vez)
        User entity = toEntity(dto);
        // Forzamos rol CLIENTE si no viene en el DTO
        if (entity.getRol() == null) {
            entity.setRol(Rol.ROL_CLIENTE);
        }
        User saved = userRepo.save(entity);
        // 4) Entidad → DTO (sin tocar la clave)
        return toDto(saved);
    }

    @Transactional
    public UserDto updateMe(UserDto dto) {
        User u = userRepo.findById(dto.getId())
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + dto.getId()));
        // Actualizar campos permitidos
        u.setNombre(dto.getNombre());
        u.setEmail(dto.getEmail());
        u.setRol(dto.getRol()); // El admin puede cambiar el rol
        if (dto.getClave() != null && !dto.getClave().isBlank()) {
             u.setClave(passwordEncoder.encode(dto.getClave()));
        }
        User updated = userRepo.save(u);
        return toDto(updated);
    }

    @Transactional(readOnly = true) // Solo lectura, mejora rendimiento
    public List<UserDto> findAllUsers() {
        return userRepo.findAll().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDto findById(Long id) {
        User u = userRepo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + id));
        return toDto(u);
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepo.existsById(id)) {
            throw new EntityNotFoundException("Usuario no encontrado con ID: " + id);
        }
        userRepo.deleteById(id);
    }

    @Transactional
public String resetPassword(Long userId) {
    User user = userRepo.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + userId));

    // Genera una contraseña temporal segura
    String temporaryPassword = UUID.randomUUID().toString().substring(0, 8);
    user.setClave(passwordEncoder.encode(temporaryPassword)); 
    userRepo.save(user);

    System.out.println("Contraseña temporal generada para " + user.getNombreUsuario() + ": " + temporaryPassword);

    return temporaryPassword;
}

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepo.findByNombreUsuario(username)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + username));

        // 1. Verificar que la contraseña actual sea correcta
        if (!passwordEncoder.matches(currentPassword, user.getClave())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta.");
        }

        // 2. Encriptar y guardar la nueva contraseña
        user.setClave(passwordEncoder.encode(newPassword));
        userRepo.save(user);
    }


    public UserDto findByNombreUsuario(String nombreUsuario) {
        User u = userRepo.findByNombreUsuario(nombreUsuario)
           .orElseThrow(() -> new EntityNotFoundException(
               "Usuario no encontrado: " + nombreUsuario));
        return toDto(u);
    }


    // ——————————————————————————————————————————————————————
    // Métodos privados de mapeado Entity ↔ DTO
    // ——————————————————————————————————————————————————————

    private User toEntity(UserDto dto) {
        User user = new User();
        user.setNombreUsuario(dto.getNombreUsuario());
        // Encriptamos la clave una sola vez aquí
        user.setClave(passwordEncoder.encode(dto.getClave()));
        user.setNombre(dto.getNombre());
        user.setEmail(dto.getEmail());
        user.setRol(dto.getRol());
        return user;
    }

    private UserDto toDto(User u) {
        UserDto dto = new UserDto();
        dto.setId(u.getId());
        dto.setNombreUsuario(u.getNombreUsuario());
        // NO encriptamos ni devolvemos la clave al cliente
        dto.setClave(null);
        dto.setNombre(u.getNombre());
        dto.setEmail(u.getEmail());
        dto.setRol(u.getRol());
        return dto;
    }

    @Transactional(readOnly = true)
public Long getIdByUsername(String username) {
    User user = userRepo.findByNombreUsuario(username)
        .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + username));
    return user.getId();
}

@Transactional
public UserDto update(UserDto dto) {
    User u = userRepo.findById(dto.getId())
        .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + dto.getId()));
    if (dto.getNombre() != null && !dto.getNombre().isBlank()) {
        u.setNombre(dto.getNombre());
    }
    if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
        u.setEmail(dto.getEmail());
    }
    User updated = userRepo.save(u);
    return toDto(updated);
}

@Transactional
public void requestPasswordResetNotification(String username) {
    Optional<User> userOptional = userRepo.findByNombreUsuario(username);

    if (userOptional.isPresent()) {
        // Usuario encontrado, enviar notificación al administrador
        sendAdminPasswordResetNotification(username);
        // No hacer nada más aquí, no enviar contraseña al usuario directamente
    } else {
        // Usuario no encontrado. Por seguridad, no lanzamos una excepción diferente
        // para evitar enumeración de usuarios. Solo registramos y no hacemos nada.
        System.out.println("Solicitud de reseteo para usuario no existente: " + username);
        // Podrías registrar esto en un log de seguridad
    }
}

private void sendAdminPasswordResetNotification(String username) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("support@aecblock.com"); // Reemplaza con tu correo de remitente
        message.setTo(adminEmail); // Correo del administrador configurado
        message.setSubject("Solicitud de Reseteo de Contraseña - Usuario: " + username);
        message.setText("El usuario '" + username + "' ha solicitado un reseteo de contraseña. " +
                        "Por favor, ve al panel de administración para generar una nueva contraseña temporal " +
                        "y enviársela manualmente al usuario.");

        mailSender.send(message);
        System.out.println("Correo de notificación a admin enviado para usuario: " + username);
    }

    @Transactional
    public String resetUserPassword(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado para resetear contraseña: " + userId));

        // Generar una contraseña temporal segura
        String temporaryPassword = UUID.randomUUID().toString().substring(0, 8); // Ejemplo: 8 caracteres
        user.setClave(passwordEncoder.encode(temporaryPassword));
        userRepo.save(user);

        // Envía el correo electrónico al usuario con la contraseña temporal
        sendTemporaryPasswordToUser(user.getEmail(), temporaryPassword);

        return temporaryPassword; // Devuelve la temporal para que el admin la vea
    } 

    private void sendTemporaryPasswordToUser(String userEmail, String temporaryPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("support@aecblock.com");
        message.setTo(userEmail);
        message.setSubject("Tu nueva contraseña temporal de acceso");
        message.setText("Hola,\n\n" +
                        "Tu contraseña ha sido reseteada por un administrador. Tu nueva contraseña temporal es:\n\n" +
                        "**" + temporaryPassword + "**\n\n" +
                        "Por favor, inicia sesión con esta contraseña y cámbiala inmediatamente por una que puedas recordar.\n\n" +
                        "Saludos,\n" +
                        "El equipo de tu aplicación.");

        mailSender.send(message);
        System.out.println("Correo de contraseña temporal enviado a: " + userEmail);
    }

}