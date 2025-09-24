package com.alert.system.controller;

import com.alert.system.dto.SystemInfo;
import com.alert.system.service.SystemInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SystemController {

    @Autowired
    private SystemInfoService systemInfoService;

    @GetMapping("/system-info")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SystemInfo> getSystemInfo() {
        SystemInfo systemInfo = systemInfoService.getSystemInfo();
        return ResponseEntity.ok(systemInfo);
    }
}