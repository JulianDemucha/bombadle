package com.bombadle.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/security")
@ConditionalOnProperty(name = "test.security.controllers.enabled", havingValue = "true")
public class TestSecurityController {

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public String adminOnly() {
        return "admin_ok";
    }

    @GetMapping("/superadmin")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public String superAdminOnly() {
        return "superadmin_ok";
    }
}