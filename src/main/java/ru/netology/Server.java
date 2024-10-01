package ru.netology;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

public class Server {
    private final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    private final ExecutorService service = Executors.newFixedThreadPool(64);

    /**
     * Запускает сервер
     */
    public void start(int port) {
        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                final var socket = serverSocket.accept();
                service.execute(() -> {
                    try (final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                         final var out = new BufferedOutputStream(socket.getOutputStream())) {
                        handler(out, in);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            service.shutdown();
        }
    }

    /**
     * Обрабатывает подлкючение
     *
     * @param out - выходной поток
     * @param in  - входной поток
     */
    private void handler(BufferedOutputStream out, BufferedReader in) {
        try {
            String parts1 = in.readLine();
            final var parts = parts1.split(" ");

            if (parts.length != 3) {
                return;
            }

            final var path = parts[1];
            if (!validPaths.contains(path)) {
                badRequest(out);
                return;
            }
            final var filePath = Path.of(".", "public", path);

            final String mimeType = Files.probeContentType(filePath);
            if (path.equals("/classic.html")) {
                final String template;
                template = Files.readString(filePath);
                final var content = template.replace("{time}", LocalDateTime.now().toString()).getBytes();
                sendResponse(out, content.length, mimeType);
                out.write(content);
                out.flush();
                return;
            }

            final var length = Files.size(filePath);
            sendResponse(out, length, mimeType);
            Files.copy(filePath, out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Формирует и отправляет ответ клиенту, при котором запрос со стороны
     * клиента является корректным и выполнен без проблем со стороны сервера
     *
     * @param out      - выходной поток
     * @param length   - длина ответа
     * @param mimeType - метатег
     */
    private static void sendResponse(BufferedOutputStream out, long length, String mimeType) {
        String status = "HTTP/1.1 200 OK";
        try {
            out.write((status + "\r\n" + "Content-Type: " + mimeType + "\r\n" + "Content-Length: " + length + "\r\n" + "Connection: close\r\n" + "\r\n").getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Формирует и отправляет ответ при неверном запросе
     *
     * @param out - входной поток
     */
    private static void badRequest(BufferedOutputStream out) {
        try {
            out.write(("HTTP/1.1 404 Not Found\r\n" +
                    "Content-Length: 0\r\n" +
                    "Connection: close\r\n" + "\r\n")
                    .getBytes());
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
