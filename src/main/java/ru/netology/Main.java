package ru.netology;


import java.util.List;
import java.io.IOException;


public class Main {
    public static void main(String[] args) {
        // Список допустимых путей
        List<String> validPaths = List.of(
                "/index.html", "/spring.svg", "/spring.png",
                "/resources.html", "/styles.css", "/app.js",
                "/links.html", "/forms.html", "/classic.html",
                "/events.html", "/events.js", "/favicon.ico"
        );

        // Создаем и запускаем сервер
        HttpServer server = new HttpServer(9999, validPaths);
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        } finally {
            server.stop();
        }
    }
}