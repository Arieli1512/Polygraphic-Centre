package com.drobnyd.drobnyd.notifications;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender emailSender;

    @Autowired
    public EmailService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void sendOrderStatusEmail(String toAddress, String status) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("drobnyd.home@gmail.com\n");
        message.setTo(toAddress);
        message.setSubject("Aktualizacja statusu zamówienia");

        // Budowanie treści z dynamicznym statusem
        String text = String.format("Szanowny Panie/Pani,\ninformujemy że status zamówienia uległ zmianie.\nObecny status to %s.", status);
        message.setText(text);

        emailSender.send(message);
    }
}