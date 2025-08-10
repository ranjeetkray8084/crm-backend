package com.example.real_estate_crm.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = {
    "https://leadstracker.in",
    "https://www.leadstracker.in", 
    "https://crm.leadstracker.in",
    "https://test.leadstracker.in",
    "http://localhost:5173"
}, allowCredentials = "true")
public class CorsTestController {

    @GetMapping("/cors")
    public ResponseEntity<Map<String, String>> testCors() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "CORS is working!");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cors")
    public ResponseEntity<Map<String, String>> testCorsPost(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "CORS POST is working!");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        response.put("received", body != null ? body.toString() : "no body");
        return ResponseEntity.ok(response);
    }
}