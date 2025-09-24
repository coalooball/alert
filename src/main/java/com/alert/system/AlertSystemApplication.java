package com.alert.system;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AlertSystemApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AlertSystemApplication.class);

        for (String arg : args) {
            if ("--help".equals(arg) || "-h".equals(arg)) {
                printHelp();
                System.exit(0);
            }
            if ("--version".equals(arg) || "-V".equals(arg)) {
                printVersion();
                System.exit(0);
            }
            if ("--init-db".equals(arg)) {
                System.setProperty("app.init-db", "true");
            }
            if (arg.startsWith("--port=") || arg.startsWith("-P=")) {
                String port = arg.substring(arg.indexOf('=') + 1);
                System.setProperty("SERVER_PORT", port);
            }
            if (arg.startsWith("--host=") || arg.startsWith("-H=")) {
                String host = arg.substring(arg.indexOf('=') + 1);
                System.setProperty("SERVER_HOST", host);
            }
        }

        ApplicationContext context = app.run(args);

        if (System.getProperty("app.init-db") != null) {
            System.out.println("🎉 Database initialization completed successfully!");
            System.out.println("💡 You can now run 'java -jar target/alert-system-0.1.0.jar' to start the server");
            System.exit(0);
        }
    }

    @Bean
    CommandLineRunner commandLineRunner() {
        return args -> {
            String host = System.getProperty("SERVER_HOST", "127.0.0.1");
            String port = System.getProperty("SERVER_PORT", "3000");

            System.out.println("🚀 Server running on http://" + host + ":" + port);
            System.out.println("📱 Alert System Frontend available at http://" + host + ":" + port);
            System.out.println("🔑 API endpoints available at http://" + host + ":" + port + "/api");
            System.out.println("👤 Default admin: admin/admin123");
            System.out.println("💡 Host: " + host + " | Port: " + port);
        };
    }

    private static void printHelp() {
        System.out.println("Alert System Web Server");
        System.out.println("Usage: java -jar alert-system.jar [options]");
        System.out.println("\nOptions:");
        System.out.println("  -P, --port <PORT>    Port to bind the server to (default: 3000)");
        System.out.println("  -H, --host <HOST>    Host address to bind the server to (default: 127.0.0.1)");
        System.out.println("  --init-db            Initialize database (drop and recreate)");
        System.out.println("  -h, --help           Show this help message");
        System.out.println("  -V, --version        Show version information");
    }

    private static void printVersion() {
        System.out.println("Alert System Version: 0.1.0");
    }
}