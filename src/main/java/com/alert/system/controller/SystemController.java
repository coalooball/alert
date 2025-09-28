package com.alert.system.controller;

import com.alert.system.dto.SystemInfo;
import com.alert.system.service.DefaultConfigService;
import com.alert.system.service.SystemInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SystemController {

    @Autowired
    private SystemInfoService systemInfoService;

    @Autowired
    private DefaultConfigService defaultConfigService;

    @GetMapping("/system-info")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SystemInfo> getSystemInfo() {
        SystemInfo systemInfo = systemInfoService.getSystemInfo();
        return ResponseEntity.ok(systemInfo);
    }

    @PostMapping("/restore-default-config")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Map<String, String>> restoreDefaultConfig() {
        try {
            defaultConfigService.restoreDefaultConfig();
            Map<String, String> response = new HashMap<>();
            response.put("message", "默认配置恢复成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "恢复默认配置失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}