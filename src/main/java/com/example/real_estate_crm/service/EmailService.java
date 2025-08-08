package com.example.real_estate_crm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    // ✅ Send OTP Email
    public void sendOtpEmail(String toEmail, String otp) {
        String subject = "Your OTP for Password Reset";
        String text = "Use the following OTP to reset your password: " + otp;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(text);

        javaMailSender.send(message);
    }

    // ✅ Send Contact Form Email
    public void sendContactMail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("conceptrealty.info@gmail.com"); 
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        javaMailSender.send(message); // ✅ FIXED
    }
}
