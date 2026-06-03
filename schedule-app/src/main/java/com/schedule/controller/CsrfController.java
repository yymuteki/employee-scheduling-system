package com.schedule.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the CSRF token so the SPA can pick it up before login.
 * Spring Security's CookieCsrfTokenRepository will set the XSRF-TOKEN cookie
 * in the response, and the client can read it from there.
 */
@RestController
public class CsrfController {

    @GetMapping("/api/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }
}
