package com.aec.aec.UsersSrv.controller;


import com.aec.aec.UsersSrv.dto.ContactoDto;
import com.aec.aec.UsersSrv.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.mail.MailException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ContactoController {

    private final EmailService emailService;

    @PostMapping("/contact")
    public ResponseEntity<String> enviarMensajeContacto(@RequestBody ContactoDto request) {
        try {
            // Llama al servicio de correo para enviar la notificación al administrador
            emailService.sendContactEmail(
                request.getNombre(),
                request.getEmail(),
                request.getAsunto(),
                request.getMensaje()
            );
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

