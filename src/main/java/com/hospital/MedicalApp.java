package com.hospital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.awt.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class MedicalApp {

    public static void main(String[] args) {
        // Ensure data directory exists before any file I/O
        try { Files.createDirectories(Path.of("data")); } catch (Exception ignored) {}

        SpringApplication.run(MedicalApp.class, args);

        // මේක තමයි මැජික් එක - සර්වර් එක ස්ටාර්ට් වුණ ගමන් බ්‍රවුසර් එක ඕපන් කරනවා
        openBrowser("http://localhost:8080");
    }

    private static void openBrowser(String url) {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.browse(new URI(url));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec("rundll32 url.dll,FileProtocolHandler " + url);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}