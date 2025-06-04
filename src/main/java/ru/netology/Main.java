package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        final var validPaths = List.of(
                "/index.html", "/spring.svg", "/spring.png",
                "/resources.html", "/styles.css", "/app.js",
                "/links.html", "/forms.html", "/classic.html",
                "/events.html", "/events.js", "/favicon.ico"
        );

        try (final var serverSocket = new ServerSocket(9999)) {
            System.out.println("Сервер запущен на порту 9999");

            while (true) {
                try {
                    final var socket = serverSocket.accept();
                    new Thread(() -> handleConnection(socket, validPaths)).start();
                } catch (IOException e) {
                    System.err.println("Ошибка подключения: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Не удалось запустить сервер: " + e.getMessage());
        }
    }

    private static void handleConnection(Socket socket, List<String> validPaths) {
        try (socket;
             var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             var out = new BufferedOutputStream(socket.getOutputStream())) {

            final var requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            System.out.println("Получен запрос: " + requestLine);

            final var parts = requestLine.split(" ");
            if (parts.length != 3) {
                sendResponse(out, 400, "Bad Request");
                return;
            }

            final var path = parts[1];
            if (!validPaths.contains(path)) {
                sendHtmlResponse(out, 404, "Not Found",
                        "<h1>404</h1><p>Страница не найдена. <a href='/index.html'>На главную</a></p>");
                return;
            }

            final var filePath = Path.of("public", path);
            if (!Files.exists(filePath)) {
                sendResponse(out, 404, "Not Found");
                return;
            }

            final var mimeType = Files.probeContentType(filePath);
            if (path.equals("/classic.html")) {
                handleClassicHtml(out, filePath, mimeType);
            } else {
                sendFile(out, filePath, mimeType);
            }

        } catch (IOException e) {
            System.err.println("Ошибка обработки соединения: " + e.getMessage());
        }
    }

    private static void sendResponse(BufferedOutputStream out, int statusCode, String statusText) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                "Content-Length: 0\r\n" +
                "Connection: close\r\n\r\n";
        out.write(response.getBytes());
        out.flush();
    }

    private static void sendHtmlResponse(BufferedOutputStream out, int statusCode,
                                         String statusText, String html) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + html.length() + "\r\n" +
                "Connection: close\r\n\r\n" +
                html;
        out.write(response.getBytes());
        out.flush();
    }

    private static void handleClassicHtml(BufferedOutputStream out, Path filePath, String mimeType) throws IOException {
        String content = Files.readString(filePath)
                .replace("{time}", LocalDateTime.now().toString());
        sendHtmlResponse(out, 200, "OK", content);
    }

    private static void sendFile(BufferedOutputStream out, Path filePath, String mimeType) throws IOException {
        long length = Files.size(filePath);
        String headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + mimeType + "\r\n" +
                "Content-Length: " + length + "\r\n" +
                "Connection: close\r\n\r\n";
        out.write(headers.getBytes());
        Files.copy(filePath, out);
        out.flush();
    }
}