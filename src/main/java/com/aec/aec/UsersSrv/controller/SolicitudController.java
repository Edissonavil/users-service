// src/main/java/com/aec/aec/UsersSrv/controller/SolicitudController.java
package com.aec.aec.UsersSrv.controller;

import com.aec.aec.UsersSrv.dto.SolicitudCreadorDTO;
import com.aec.aec.UsersSrv.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor; // Añadir si usas inyección por constructor final

@RestController
@RequestMapping("/api") // Puedes cambiar esto a /api/solicitudes o similar si prefieres
@RequiredArgsConstructor // Para inyección de dependencias con final
@CrossOrigin(origins = "http://localhost:3000") // Asegúrate de que tu puerto de frontend sea correcto
public class SolicitudController {

    private final EmailService emailService; // Inyección por constructor

    @PostMapping("/solicitud-creador")
    public ResponseEntity<String> solicitarCreador(@RequestBody SolicitudCreadorDTO solicitud) {
        try {
            emailService.sendCreatorApplicationEmail(
                solicitud.getNombreCompleto(),
                solicitud.getUsername(),
                solicitud.getEmail()
            );
            return ResponseEntity.ok("Solicitud enviada exitosamente. El administrador ha sido notificado.");
        } catch (MailException e) {
            System.err.println("Error al enviar correo de solicitud: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error al procesar la solicitud. No se pudo enviar el correo de notificación.");
        } catch (Exception e) {
            System.err.println("Error inesperado al procesar solicitud: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Ocurrió un error inesperado. Por favor, inténtalo de nuevo más tarde.");
        }
    }
}