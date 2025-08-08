package com.example.real_estate_crm.Controller;


import com.example.real_estate_crm.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.example.real_estate_crm.model.ContactRequest;

@RestController
@RequestMapping("/api/contact")
@CrossOrigin(origins = "*") // Allow React frontend access
public class ContactController {

    @Autowired
    private EmailService emailService;

    @PostMapping
    public String handleContactForm(@RequestBody ContactRequest request) {
        String subject = "New Contact Message from " + request.getName();
        String body = "You have received a new message:\n\n"
                    + "Name: " + request.getName() + "\n"
                    + "Email: " + request.getEmail() + "\n\n"
                    + "Message:\n" + request.getMessage();

        // 📨 Send email to your address
        emailService.sendContactMail("conceptrealty.info@gmail.com", subject, body); 

        return "Message sent successfully!";
    }
}
