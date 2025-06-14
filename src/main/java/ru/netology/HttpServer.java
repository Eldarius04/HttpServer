package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

public class HttpServer {
    private final int port;
    private final List<String> validPaths;
    private ServerSocket serverSocket;

    public HttpServer(int port, List<String> validPaths) {
        this.port = port;
        this.validPaths = validPaths != null ? new ArrayList<>(validPaths) : new ArrayList<>();
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);

        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                handleConnection(socket);
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("Connection error: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Server stopped");
            }
        } catch (IOException e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }

    private void handleConnection(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream())) {

            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                return;
            }

            String[] parts = requestLine.split(" ");
            if (parts.length != 3) {
                sendResponse(out, 400, "Bad Request");
                return;
            }

            String method = parts[0];
            String fullPath = parts[1];

            Map<String, String> headers = new HashMap<>();
            String headerLine;
            while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
                int idx = headerLine.indexOf(":");
                if (idx > 0) {
                    headers.put(headerLine.substring(0, idx).trim(),
                            headerLine.substring(idx + 1).trim());
                }
            }

            Request request = new Request(method, fullPath, headers, socket.getInputStream());
            System.out.println("Request: " + request.getMethod() + " " + request.getPath());

            if (!request.getQueryParams().isEmpty()) {
                System.out.println("Query params: " + request.getQueryParams());
            }

            processRequest(request, out);

        } catch (IOException e) {
            System.err.println("Error handling connection: " + e.getMessage());
        }
    }

    private void processRequest(Request request, BufferedOutputStream out) throws IOException {
        if (!validPaths.contains(request.getPath())) {
            sendHtmlResponse(out, 404, "Not Found",
                    "<h1>404</h1><p>Page not found. <a href='/index.html'>Go to main page</a></p>");
            return;
        }

        Path filePath = Path.of("public", request.getPath());
        if (!Files.exists(filePath)) {
            sendResponse(out, 404, "Not Found");
            return;
        }

        String mimeType = Files.probeContentType(filePath);
        if (request.getPath().equals("/classic.html")) {
            handleClassicHtml(request, out, filePath, mimeType);
        } else {
            sendFile(out, filePath, mimeType);
        }
    }

    private void sendResponse(BufferedOutputStream out, int statusCode, String statusText) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                "Content-Length: 0\r\n" +
                "Connection: close\r\n\r\n";
        out.write(response.getBytes());
        out.flush();
    }

    private void sendHtmlResponse(BufferedOutputStream out, int statusCode, String statusText, String html) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + html.length() + "\r\n" +
                "Connection: close\r\n\r\n" +
                html;
        out.write(response.getBytes());
        out.flush();
    }

    private void handleClassicHtml(Request request, BufferedOutputStream out, Path filePath, String mimeType) throws IOException {
        String content = Files.readString(filePath)
                .replace("{time}", LocalDateTime.now().toString());

        Optional<String> nameParam = request.getQueryParam("name");
        if (nameParam.isPresent()) {
            content = content.replace("{name}", nameParam.get());
        }

        sendHtmlResponse(out, 200, "OK", content);
    }

    private void sendFile(BufferedOutputStream out, Path filePath, String mimeType) throws IOException {
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