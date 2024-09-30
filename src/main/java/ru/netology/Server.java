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
    private final ExecutorService service = Executors.newFixedThreadPool(60);


    /**
     * Запускает сервер
     */
    public void start() {
        try (final var serverSocket = new ServerSocket(9999)) {
            while (true) {
                try (
                        final var socket = serverSocket.accept();
                        final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        final var out = new BufferedOutputStream(socket.getOutputStream());
                ) {
                    // read only request line for simplicity
                    // must be in form GET /path HTTP/1.1

                    Callable<String> task = () -> in.readLine();
                    Future<String> result = service.submit(task);
                    method(out, result);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            service.shutdown();
        }
    }


    /**
     * Обрабатывает конкретное подлкючение
     *
     * @param out
     */
    void method(BufferedOutputStream out, Future<String> result) {
        final String parts1;
        try {
            parts1 = result.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        final var parts = parts1.split(" ");

        if (parts.length != 3) {
            // just close socket
            return;
        }
        final var path = parts[1];
        if (!validPaths.contains(path)) {
            try {
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                out.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        final var filePath = Path.of(".", "public", path);
        final String mimeType;
        try {
            mimeType = Files.probeContentType(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // special case for classic
        if (path.equals("/classic.html")) {
            final String template;
            try {
                template = Files.readString(filePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            try {
                sendResponse(out, "HTTP/1.1 200 OK", content.length, mimeType);
                out.write(content);

                out.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        try {
            final var length = Files.size(filePath);

            sendResponse(out, "HTTP/1.1 200 OK", length, mimeType);

            Files.copy(filePath, out);

            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Формирует и отправляет ответ клиенту
     *
     * @param out      - выходной поток
     * @param status   - статус ответа
     * @param length   - длина ответа
     * @param mimeType - метатег
     * @throws IOException
     */
    private static void sendResponse(BufferedOutputStream out, String status, long length, String mimeType) throws IOException {
        out.write((status + "\r\n" +
                "Content-Type: " + mimeType + "\r\n" +
                "Content-Length: " + length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n").getBytes());
    }
}
