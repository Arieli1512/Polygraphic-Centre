package com.drobnyd.drobnyd.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {

    // Anyone can access this
    @GetMapping("/public/hello")
    public String publicHello() {
        return "Hello from a public endpoint! No login required.";
    }

    // ONLY logged-in users with a valid token can access this
    @GetMapping("/private/dashboard")
    public String privateDashboard() {
        return "Welcome to the hidden dashboard! Your Firebase token is valid.";
    }
}
