package com.project.expense_tracker_backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("pageTitle", "Welcome Home");
        // This will render an index.html page that uses layout.html
        return "index"; 
    }
}
