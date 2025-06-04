package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final List<String> validPaths;
    private final ExecutorService threadPool;
    private final Map<String, Map<String, Handler>> handlers;

    public Server(List<String> validPaths) {
        this.validPaths = validPaths;
        this.threadPool = Executors.newFixedThreadPool(64);
        this.handlers = new ConcurrentHashMap<>();
    }

    public void addHandler(String method, String path, Handler handler) {
        handlers.computeIfAbsent(method, k -> new ConcurrentHashMap<>()).put(path, handler);
    }

    public void listen(int port) {
        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                final var socket = serverSocket.accept();
                threadPool.execute(() -> processConnection(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processConnection(Socket socket) {
        try (
                socket;
                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream());
        ) {
            final var requestLine = in.readLine();
            if (requestLine == null) return;

            final var parts = requestLine.split(" ");
            if (parts.length != 3) return;

            final var method = parts[0];
            final var path = parts[1];

            // Чтение заголовков
            Map<String, String> headers = new HashMap<>();
            String headerLine;
            while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
                var index = headerLine.indexOf(":");
                if (index > 0) {
                    headers.put(headerLine.substring(0, index).trim().toLowerCase(),
                            headerLine.substring(index + 1).trim());
                }
            }

            // Проверка обработчиков
            Handler handler = findHandler(method, path);
            if (handler != null) {
                handler.handle(new Request(method, path, headers, socket.getInputStream()), out);
                return;
            }

            // Обработка статических файлов
            if (!validPaths.contains(path)) {
                sendNotFound(out);
                return;
            }

            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);

            if (path.equals("/classic.html")) {
                sendClassicHtml(out, filePath, mimeType);
            } else {
                sendFile(out, filePath, mimeType);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Handler findHandler(String method, String path) {
        var methodHandlers = handlers.get(method);
        return methodHandlers != null ? methodHandlers.get(path) : null;
    }

    private void sendNotFound(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private void sendClassicHtml(BufferedOutputStream out, Path filePath, String mimeType) throws IOException {
        final var template = Files.readString(filePath);
        final var content = template.replace(
                "{time}",
                LocalDateTime.now().toString()
        ).getBytes();
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + content.length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.write(content);
        out.flush();
    }

    private void sendFile(BufferedOutputStream out, Path filePath, String mimeType) throws IOException {
        final var length = Files.size(filePath);
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        Files.copy(filePath, out);
        out.flush();
    }
}