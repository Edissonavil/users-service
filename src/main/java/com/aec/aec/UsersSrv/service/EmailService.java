// src/main/java/com/aec/aec/UsersSrv/service/EmailService.java
package com.aec.aec.UsersSrv.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; // Para inyectar el correo del admin desde properties
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${admin.email}")
    private String adminEmail;  

    public void sendCreatorApplicationEmail(String nombreCompleto, String username, String email, String hablanosDeTi)
            throws MailException {
        if (adminEmail == null || adminEmail.isBlank()) {
            throw new IllegalStateException("El correo del administrador no está configurado (admin.email.recipient).");
        }

        StringBuilder emailText = new StringBuilder();
        emailText.append("Se ha recibido una nueva solicitud para ser Creador AEC con los siguientes datos:\n\n");
        emailText.append("Nombre Completo: ").append(nombreCompleto).append("\n");
        emailText.append("Nombre de Usuario Sugerido: ").append(username).append("\n");
        emailText.append("Correo Electrónico: ").append(email).append("\n");

        if (hablanosDeTi != null && !hablanosDeTi.trim().isEmpty()) {
            emailText.append("Háblanos un poco de ti: \n").append(hablanosDeTi).append("\n");
        } else {
            emailText.append("Háblanos un poco de ti: El usuario no proporcionó información adicional.\n");
        }
        emailText.append("\nPor favor, revisa esta solicitud y contacta al interesado.");

        // Usamos sendHtmlEmail para mantener la coherencia si quieres usar HTML en el
        // futuro
        // Por ahora, el contenido es plano, pero MimeMessageHelper lo permite.
        String subject = "📩 Nueva Solicitud de Creador AEC";
        String htmlContent = String.format(
                "<html>" +
                        "<body>" +
                        "<p>Se ha recibido una nueva solicitud para ser Creador AEC con los siguientes datos:</p>" +
                        "<ul>" +
                        "<li><strong>Nombre Completo:</strong> %s</li>" +
                        "<li><strong>Nombre de Usuario Sugerido:</strong> %s</li>" +
                        "<li><strong>Correo Electrónico:</strong> %s</li>" +
                        "<li><strong>Háblanos un poco de ti:</strong> %s</li>" +
                        "</ul>" +
                        "<p>Por favor, revisa esta solicitud y contacta al interesado.</p>" +
                        "<p>Saludos cordiales,<br/>El sistema AECBlock</p>" +
                        "</body>" +
                        "</html>",
                nombreCompleto, username, email,
                (hablanosDeTi != null && !hablanosDeTi.trim().isEmpty()) ? hablanosDeTi.replace("\n", "<br/>")
                        : "El usuario no proporcionó información adicional.");

        sendHtmlEmail(adminEmail.trim(), subject, htmlContent);
        // log.info("Correo de solicitud de creador enviado a: {}",
        // adminEmailRecipient);
    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("No hay destinatario para el email: {}", subject);
            return;
        }
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "utf-8");
            helper.setFrom(adminEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(msg);
            log.info("Email enviado a {}: {}", toEmail, subject);
        } catch (MessagingException e) {
            log.error("Error enviando email a {}: {}", toEmail, e.getMessage(), e);
        }
    }

    public void sendTemporaryPasswordEmail(String recipientEmail, String temporaryPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(recipientEmail);
            helper.setFrom(adminEmail);
            helper.setSubject("Tu Contraseña Temporal - AECBlock");

            String htmlContent = "<h1>Hola!</h1>"
                    + "<p>Tu nueva contraseña temporal para acceder a tu cuenta es:</p>"
                    + "<h2>" + temporaryPassword + "</h2>"
                    + "<p>Por favor, inicia sesión con esta contraseña y cámbiala lo antes posible por una de tu elección.</p>"
                    + "<p>Gracias,</p>"
                    + "<p>El equipo de AECBlock</p>";
            helper.setText(htmlContent, true);

            mailSender.send(message);
            System.out.println("Email con contraseña temporal enviado a: " + recipientEmail);
        } catch (jakarta.mail.MessagingException e) {
            System.err.println(
                    "Error al enviar email de contraseña temporal a " + recipientEmail + ": " + e.getMessage());
        }
    }

    public void sendContactEmail(String nombre, String email, String asunto, String mensaje) throws MailException {
        if (adminEmail == null || adminEmail.isBlank()) {
            throw new IllegalStateException("El correo del administrador no está configurado (admin.email.recipient).");
        }

        String subject = "🙋 Nuevo Mensaje de Contacto: " + asunto;
        String htmlContent = String.format(
                "<html>" +
                        "<body>" +
                        "<h2>Se ha recibido un nuevo mensaje de contacto:</h2>" +
                        "<ul>" +
                        "<li><strong>Nombre del Remitente:</strong> %s</li>" +
                        "<li><strong>Correo Electrónico del Remitente:</strong> %s</li>" +
                        "<li><strong>Asunto:</strong> %s</li>" +
                        "</ul>" +
                        "<h3>Mensaje:</h3>" +
                        "<p style='white-space: pre-line;'>%s</p>" +
                        "<p>Por favor, responde a este mensaje directamente al correo del remitente.</p>" +
                        "<p>Saludos cordiales,<br/>El sistema AECBlock</p>" +
                        "</body>" +
                        "</html>",
                nombre, email, asunto,
                mensaje != null ? mensaje.replace("\n", "<br/>") : "El usuario no proporcionó un mensaje.");

        sendHtmlEmail(adminEmail.trim(), subject, htmlContent);
        log.info("Correo de contacto enviado a: {}", adminEmail);
    }

    public void sendCollaboratorWelcomeEmail(String toEmail, String nombre, String username, String temporaryPassword) {
        String subject = "Bienvenido(a) a AECBlock - Credenciales de acceso (clave temporal)";
        String safeNombre = (nombre != null && !nombre.isBlank()) ? nombre : username;

        String html = """
                <html>
                <body style="font-family: Arial, sans-serif; line-height:1.6;">
                  <h2>¡Bienvenido(a) a AECBlock!</h2>
                  <p>Hola <strong>%s</strong>,</p>
                  <p>Tu cuenta de <strong>Colaborador</strong> ha sido creada exitosamente. A continuación, tus credenciales:</p>
                  <ul>
                    <li><strong>Usuario:</strong> %s</li>
                    <li><strong>Contraseña temporal:</strong> %s</li>
                  </ul>
                  <p><strong>Importante:</strong> esta contraseña es de <strong>uso temporal</strong>. Por favor, inicia sesión y <strong>cámbiala de inmediato</strong> desde tu perfil.</p>
                  <p>Gracias por formar parte del grupo de especialistas que acelera el crecimiento de la próxima generación de profesionales AEC.</p>
                  <p>Saludos cordiales,<br/>Equipo AECBlock</p>
                </body>
                </html>
                """
                .formatted(safeNombre, username, temporaryPassword);

        sendHtmlEmail(toEmail, subject, html);
    }

}
