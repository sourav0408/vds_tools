package com.example.vds_tools.controller;

import com.example.vds_tools.service.WindowsPrivateKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
@Controller

public class HomeFinal {
    @Autowired
    private WindowsPrivateKeyService privateKeyService;
    @GetMapping("/") // This maps the form to the /vds URL
    public String showForm(Model model)  {
        // Return the view (Thymeleaf template)
        return "home_final";  // Ensure this matches your actual Thymeleaf template name
    }
}
