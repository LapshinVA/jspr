package ru.netology;

import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    
    public static void main(String[] args) {
        final var server = new Server();

        server.addHandler(MethodName.GET, "/index.html",
                (request, responseStream) -> {
                    try {
                        final var filePath = Path.of(".", "public", request.getPath());
                        final String mimeType = Files.probeContentType(filePath);
                        final var length = Files.size(filePath);
                        server.response(responseStream, filePath, mimeType, length);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        server.addHandler(MethodName.POST, "/index.html", (request, responseStream) -> {
            try {
                final var filePath = Path.of(".", "public", request.getPath());
                final String mimeType = Files.probeContentType(filePath);
                final var length = Files.size(filePath);
                server.response(responseStream, filePath, mimeType, length);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        server.start(9999);
    }
}