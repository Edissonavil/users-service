// src/main/java/com/aec/aec/UsersSrv/service/EmailService.java
package com.aec.aec.UsersSrv.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; // Para inyectar el correo del admin desde properties
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // Inyecta el correo del administrador desde application.yml/properties
    @Value("${admin.email.recipient:support@aecblock.com}")
    private String adminEmailRecipient;

    public void sendCreatorApplicationEmail(String nombreCompleto, String username, String email) throws MailException {
    if (adminEmailRecipient == null || adminEmailRecipient.isBlank()) {
        throw new IllegalStateException("El correo del administrador no está configurado.");
    }

    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(adminEmailRecipient.trim());
    message.setFrom("support@aecblock.com");
    message.setSubject("Nueva Solicitud de Creador AEC");
    message.setText(
        "Se ha recibido una nueva solicitud para ser Creador AEC con los siguientes datos:\n\n" +
        "Nombre Completo: " + nombreCompleto + "\n" +
        "Nombre de Usuario Sugerido: " + username + "\n" +
        "Correo Electrónico: " + email + "\n\n" +
        "Por favor, revisa esta solicitud y contacta al interesado."
    );

    mailSender.send(message);
}


public void sendTemporaryPasswordEmail(String recipientEmail, String temporaryPassword) {
    try {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(recipientEmail);
        helper.setFrom("support@aecblock.com"); 
        helper.setSubject("Tu Contraseña Temporal - AECBlock");

        String htmlContent = "<h1>Hola!</h1>"
                           + "<p>Tu nueva contraseña temporal para acceder a tu cuenta es:</p>"
                           + "<h2>" + temporaryPassword + "</h2>"
                           + "<p>Por favor, inicia sesión con esta contraseña y cámbiala lo antes posible por una de tu elección.</p>"
                           + "<p>Gracias,</p>"
                           + "<p>El equipo de Productos Aec</p>";
        helper.setText(htmlContent, true);

        mailSender.send(message);
        System.out.println("Email con contraseña temporal enviado a: " + recipientEmail);
    } catch (jakarta.mail.MessagingException e) {
        System.err.println("Error al enviar email de contraseña temporal a " + recipientEmail + ": " + e.getMessage());
    }
}

}
