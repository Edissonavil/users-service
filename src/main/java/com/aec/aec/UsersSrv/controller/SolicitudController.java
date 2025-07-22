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
public class SolicitudController {

    private final EmailService emailService; // Inyección por constructor

    @PostMapping("/solicitud-creador")
    public ResponseEntity<String> solicitarCreador(@RequestBody SolicitudCreadorDTO request) {
        try {
            // Llama al servicio de correo para enviar la notificación al administrador
            emailService.sendCreatorApplicationEmail(
                request.getNombreCompleto(),
                request.getUsername(),
                request.getEmail(),
                request.getHablanosDeTi() 
            );
            return ResponseEntity.ok("Solicitud enviada con éxito.");
        } catch (MailException e) {
            // Manejo de errores de envío de correo
            return ResponseEntity.internalServerError().body("Error al enviar la solicitud: " + e.getMessage());
        } catch (IllegalStateException e) {
            // Manejo de errores de configuración (ej. adminEmailRecipient no configurado)
            return ResponseEntity.internalServerError().body(e.getMessage());
        } catch (Exception e) {
            // Otros errores inesperados
            return ResponseEntity.internalServerError().body("Ocurrió un error inesperado al procesar la solicitud.");
        }
    }
}