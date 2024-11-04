package com.smart.service;

import java.util.Properties;

import org.springframework.stereotype.Service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    public boolean sendEmail(String subject, String message, String to) {
        boolean f = false;

        String from = "techsoftindia2018@gmail.com";

        // SMTP server information
        String host = "smtp.gmail.com";

        // Get system properties
        Properties properties = System.getProperties();

        // Set SMTP server properties
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");

        // Step 1: Get the session object
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("techsoftindia2018@gmail.com", "123@SoftTech");
            }
        });

        session.setDebug(true);

        // Step 2: Compose the message
        MimeMessage m = new MimeMessage(session);

        try {
            // From email
            m.setFrom(from);

            // Adding recipient to message
            m.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

            // Adding subject to message
            m.setSubject(subject);

            // Adding HTML content to message
            m.setContent(message, "text/html");

            // Step 3: Send the message using Transport class
            Transport.send(m);

            System.out.println("Sent successfully.");
            f = true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return f;
    }
}
