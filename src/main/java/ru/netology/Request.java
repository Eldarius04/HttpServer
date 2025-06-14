package ru.netology;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Класс для представления HTTP-запроса
 */
public class Request {
    private final String method;
    private final String path;
    private final String queryString;
    private final Map<String, String> headers;
    private final InputStream body;
    private final Map<String, List<String>> queryParams;

    /**
     * Конструктор для создания объекта запроса
     * @param method HTTP-метод (GET, POST и т.д.)
     * @param path Полный путь включая query-параметры
     * @param headers Заголовки запроса
     * @param body Тело запроса
     */
    public Request(String method, String path, Map<String, String> headers, InputStream body) {
        this.method = method;
        this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
        this.body = body;

        // Разделяем путь и query-параметры
        int queryIndex = path.indexOf('?');
        if (queryIndex != -1) {
            this.path = path.substring(0, queryIndex);
            this.queryString = path.substring(queryIndex + 1);
        } else {
            this.path = path;
            this.queryString = "";
        }

        this.queryParams = parseQueryParams(this.queryString);
    }

    /**
     * Парсит query-строку в Map параметров
     * @param query Строка параметров после знака ?
     * @return Map с параметрами
     */
    private Map<String, List<String>> parseQueryParams(String query) {
        Map<String, List<String>> params = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }

        List<NameValuePair> pairs = URLEncodedUtils.parse(query, StandardCharsets.UTF_8);
        for (NameValuePair pair : pairs) {
            params.computeIfAbsent(pair.getName(), k -> new ArrayList<>())
                    .add(pair.getValue());
        }

        return params;
    }

    // Геттеры

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getQueryString() {
        return queryString;
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public InputStream getBody() {
        return body;
    }

    /**
     * Получает первое значение параметра по имени
     * @param name Имя параметра
     * @return Optional со значением параметра
     */
    public Optional<String> getQueryParam(String name) {
        return queryParams.containsKey(name) && !queryParams.get(name).isEmpty()
                ? Optional.of(queryParams.get(name).get(0))
                : Optional.empty();
    }

    /**
     * Получает все значения параметра по имени
     * @param name Имя параметра
     * @return Список значений
     */
    public List<String> getQueryParams(String name) {
        return queryParams.getOrDefault(name, Collections.emptyList());
    }

    /**
     * Получает все query-параметры
     * @return Map всех параметров
     */
    public Map<String, List<String>> getQueryParams() {
        return Collections.unmodifiableMap(queryParams);
    }

    /**
     * Читает тело запроса как строку
     * @return Содержимое тела запроса
     * @throws IOException при ошибках чтения
     */
    public String getBodyAsString() throws IOException {
        StringBuilder bodyBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(body))) {
            String line;
            while ((line = reader.readLine()) != null) {
                bodyBuilder.append(line);
            }
        }
        return bodyBuilder.toString();
    }
}