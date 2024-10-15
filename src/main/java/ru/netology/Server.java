package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

public class Server implements Handler {
    private final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    private final ExecutorService service = Executors.newFixedThreadPool(64);
    private final Map<MethodName, Map<String, Handler>> handlers = new ConcurrentHashMap<>();


    /**
     * Запускает сервер
     */
    public void start(int port) {
        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                service.execute(() -> {
                    try (var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                         var out = new BufferedOutputStream(socket.getOutputStream())) {
                        handler(out, in);
                    } catch (Exception e) {
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
     * Обрабатывает подключение
     * Парсит HTTP-сообщение
     *
     * @param out - выходной поток
     * @param in  - входной поток
     */
    private void handler(BufferedOutputStream out, BufferedReader in) throws Exception {
        try {
            String requestLine = in.readLine();
            String headerLine;
            Map<String, String> headers = new HashMap<>();
            while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
                String[] parts = headerLine.split(":");
                headers.put(parts[0].trim(), parts[1].trim());
            }
            String resourcePath;
            MethodName methodName;

            if (requestLine == null) {
                badRequest(out);
                return;
            }
            String[] parts = requestLine.split(" ");
            methodName = null;
            if (parseMethodName(parts[0]).isPresent()) {
                methodName = parseMethodName(parts[0]).get();
            }
            resourcePath = parts[1];

            if (!validPaths.contains(resourcePath)) {
                badRequest(out);
                return;
            }

            Request request = new Request(methodName, resourcePath, headers);


            Body body;
            String contentLengthStr = request.getHeaders().get("Content-Length");


            int contentLength;
            if (contentLengthStr != null) {
                try {
                    contentLength = Integer.parseInt(contentLengthStr);
                    if (contentLength > 0) {
                        body = new Body(in);
                        request.setBody(body);
                    }

                } catch (NumberFormatException e) {
                    throw new IOException("Некорректный или отсутствующий заголовок Content-Length");
                }
            }
            Map<String, Handler> map = handlers.get(request.getNameMethod());
            if (map != null) {
                Handler handler = map.get(request.getPath());
                if (handler != null) {
                    handler.handle(request, out);
                }
            }
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

    private Optional<MethodName> parseMethodName(String name) {
        switch (name) {
            case "GET":
                return Optional.of(MethodName.GET);
            case "PUT":
                return Optional.of(MethodName.PUT);
            case "PATCH":
                return Optional.of(MethodName.PATCH);
            case "POST":
                return Optional.of(MethodName.POST);
            case "DELETE":
                return Optional.of(MethodName.DELETE);
            default:
                return Optional.empty();
        }
    }

    /**
     * Добавляет обработчик в мапу
     *
     * @param method  - имя метода
     * @param path    - путь
     * @param handler - обработчик
     */
    public void addHandler(MethodName method, String path, Handler handler) {
        Map<String, Handler> map = new HashMap<>();
        map.put(path, handler);
        handlers.put(method, map);
    }

    @Override
    public void handle(Request request, BufferedOutputStream responseStream) {
    }

    public void response(BufferedOutputStream responseStream, Path filePath, String mimeType, long length) {
        try {
            String status = "HTTP/1.1 200 OK";
            responseStream.write((status + "\r\n" + "Content-Type: " + mimeType + "\r\n" + "Content-Length: " + length + "\r\n" + "Connection: close\r\n" + "\r\n").getBytes());
            Files.copy(filePath, responseStream);
            responseStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
