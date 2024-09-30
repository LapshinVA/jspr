package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) {
        final var validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
        ExecutorService service = Executors.newFixedThreadPool(60);
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
                    final var parts1 = result.get();

                    final var parts = parts1.split(" ");

                    if (parts.length != 3) {
                        // just close socket
                        continue;
                    }
                    final var path = parts[1];
                    if (!validPaths.contains(path)) {
                        out.write((
                                "HTTP/1.1 404 Not Found\r\n" +
                                        "Content-Length: 0\r\n" +
                                        "Connection: close\r\n" +
                                        "\r\n"
                        ).getBytes());
                        out.flush();
                        continue;
                    }
                    final var filePath = Path.of(".", "public", path);
                    final var mimeType = Files.probeContentType(filePath);

                    // special case for classic
                    if (path.equals("/classic.html")) {
                        final var template = Files.readString(filePath);
                        final var content = template.replace(
                                "{time}",
                                LocalDateTime.now().toString()
                        ).getBytes();
                        sendResponse(out, "HTTP/1.1 200 OK", content.length, mimeType);
                        out.write(content);
                        out.flush();
                        continue;
                    }

                    final var length = Files.size(filePath);

                    sendResponse(out, "HTTP/1.1 200 OK", length, mimeType);

                    Files.copy(filePath, out);
                    out.flush();
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            service.shutdown();
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