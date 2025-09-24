package com.alert.system.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import javax.sql.DataSource;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class SystemInfoService {

    @Value("${app.version}")
    private String appVersion;

    @Value("${server.port}")
    private int serverPort;

    @Value("${server.address}")
    private String serverAddress;

    private final DataSource dataSource;

    public SystemInfoService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public com.alert.system.dto.SystemInfo getSystemInfo() {
        oshi.SystemInfo si = new oshi.SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        OperatingSystem os = si.getOperatingSystem();
        GlobalMemory memory = hal.getMemory();

        boolean dbConnected = false;
        try {
            dbConnected = !dataSource.getConnection().isClosed();
        } catch (Exception e) {
            // Database not connected
        }

        String hostname = "Unknown";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            // Unable to get hostname
        }

        return com.alert.system.dto.SystemInfo.builder()
                .systemName(os.getFamily())
                .systemVersion(os.getVersionInfo().toString())
                .kernelVersion(os.getVersionInfo().getBuildNumber())
                .hostname(hostname)
                .architecture(System.getProperty("os.arch"))
                .cpuCores(hal.getProcessor().getLogicalProcessorCount())
                .totalMemory(memory.getTotal())
                .availableMemory(memory.getAvailable())
                .bootTime(os.getSystemBootTime())
                .uptime(os.getSystemUptime())
                .currentTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .appVersion(appVersion)
                .databaseConnected(dbConnected)
                .serverAddress(serverAddress)
                .serverPort(serverPort)
                .build();
    }
}