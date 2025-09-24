package com.alert.system.controller;

import com.alert.system.dto.LoginRequest;
import com.alert.system.dto.LoginResponse;
import com.alert.system.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        try {
            LoginResponse loginResponse = userService.login(request);

            // Set JWT token in cookie
            Cookie cookie = new Cookie("token", loginResponse.getToken());
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(24 * 60 * 60); // 24 hours
            response.addCookie(cookie);

            return ResponseEntity.ok(loginResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(null);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // Clear the token cookie
        Cookie cookie = new Cookie("token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        Map<String, String> result = new HashMap<>();
        result.put("message", "Logged out successfully");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", request.getAttribute("userId"));
        userInfo.put("username", request.getAttribute("username"));
        userInfo.put("role", request.getAttribute("role"));

        return ResponseEntity.ok(userInfo);
    }
}