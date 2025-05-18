package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static final int SERVERPORT = 9999;
    private final List<String> validPaths;
    private final ExecutorService threadPool;
    private final Set<Resource> resourceSet;
    private Map<Resource, Handler> handlers;

    public Server() {
        this.validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
        this.threadPool = Executors.newFixedThreadPool(64);
        this.resourceSet = new CopyOnWriteArraySet<>();
        this.handlers = new ConcurrentHashMap<>();
    }

    public List<String> getValidPaths() {
        return validPaths;
    }

    public Set<Resource> getResourceSet() {
        return resourceSet;
    }

    public Map<Resource, Handler> getHandlers() {
        return handlers;
    }

    public void addHandler(String method, String path, Handler handler) {
        Resource resource = new Resource(method, path);
        resourceSet.add(resource);
        handlers.put(resource, handler);
    }

    protected void start() {
        try (final var serverSocket = new ServerSocket(SERVERPORT)) {
            while (true) {
                try {
                    final var clientSocket = serverSocket.accept();
                    var clientThread = new ClientThread(clientSocket);
                    threadPool.submit(clientThread);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        threadPool.shutdown();
    }

    private class ClientThread implements Runnable {
        private final Socket socket;

        public ClientThread(Socket socket) {
            this.socket = socket;
        }

        // from google guava with modifications
        private static int indexOf(byte[] array, byte[] target, int start, int max) {
            outer:
            for (int i = start; i < max - target.length + 1; i++) {
                for (int j = 0; j < target.length; j++) {
                    if (array[i + j] != target[j]) {
                        continue outer;
                    }
                }
                return i;
            }
            return -1;
        }

        private static void badRequest(BufferedOutputStream out) throws IOException {
            out.write((
                    "HTTP/1.1 400 Bad Request\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        }

        @Override
        public void run() {
            try (final var in = new BufferedInputStream(socket.getInputStream());
                 final var out = new BufferedOutputStream(socket.getOutputStream())
            ) {
                final Request request = new Request();

                final var limit = 4096;
                in.mark(limit);

                final var buffer = new byte[limit];
                final var read = in.read(buffer); // int - размер полученных байтов

                // ищем request line
                final var requestLineDelimiter = new byte[]{'\r', '\n'};
                final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
                if (requestLineEnd == -1) {
                    badRequest(out);
                    Thread.currentThread().interrupt();
                }

                // читаем requestLine
                final String[] requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
                if (requestLine.length != 3) {
                    badRequest(out);
                    Thread.currentThread().interrupt();
                }
                // парсим requestLine и сохраняем в request
                //method
                request.setMethod(requestLine[0]);
                // версия http
                request.setVersion(requestLine[2]);
                // с path придётся повозиться
                //    final var reqPath = requestLine[1];
                if (!requestLine[1].startsWith("/")) {
                    badRequest(out);
                    Thread.currentThread().interrupt();
                }
                //ищем в пути '?' и '#', если не найдём - то считаем их индексами длину строки
                int qIdx = requestLine[1].length(); //индекс '?' в пути
                int fIdx = requestLine[1].length(); //индекс '#' в пути
                if (requestLine[1].contains("?")) qIdx = requestLine[1].indexOf('?');
                if (requestLine[1].contains("#")) fIdx = requestLine[1].indexOf('#');

                //путь к ресурсу без qeryString и fragment
                String path = requestLine[1].substring(0, qIdx);
                request.setPath(path);

                //queryString - если нету - то пустая строка
                String queryString = "";
                if (qIdx != requestLine[1].length()) queryString = requestLine[1].substring(qIdx + 1, fIdx);
                request.setQueryString(queryString);
                //fragment - если нету - то пустая строка
                String fragment = "";
                if (fIdx != requestLine[1].length()) fragment = requestLine[1].substring(fIdx + 1);
                request.setFragment(fragment);

                // ищем заголовки
                final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
                final var headersStart = requestLineEnd + requestLineDelimiter.length;
                final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
                if (headersEnd == -1) {
                    badRequest(out);
                    Thread.currentThread().interrupt();
                }

                // отматываем на начало буфера
                in.reset();
                // пропускаем requestLine
                in.skip(headersStart);

                final var headersBytes = in.readNBytes(headersEnd - headersStart);
                final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
                //теперь парсим заголовки в мапу объекта request
                for (String line : headers) {
                    int splitIndex = line.indexOf(':');
                    String key = line.substring(0, splitIndex);
                    //splitIndex + 2 - отсекаем само двоеточие и пробел после него
                    String value = line.substring(splitIndex + 2);
                    request.addHeaders(key, value);
                }

                // для GET тела нет
                if (!request.getMethod().equals("GET")) {
                    in.skip(headersDelimiter.length);
                }
                // вычитываем Content-Length, чтобы прочитать body
                final String contentLength = request.getHeaders().get("Content-Length");
                if (contentLength != null) {
                    final var length = Integer.parseInt(contentLength);
                    final var bodyBytes = in.readNBytes(length);
                    request.setBody(bodyBytes);
                }

                //Тут мы завершили формирование объекта класса Request, выведем ка его в консоль поглазеть
                System.out.println(request);
                //поглядим в queryString все параметры
                System.out.println("Вся QueryString " + request.getQueryParams());
                //поищем в queryString значения по полю value
                System.out.println("Только поля с name = value: " + request.getQueryParam("value"));

                String method = request.getMethod(); //получаем метод запроса
                path = request.getPath(); // получаем адрес ресурса
                Resource resource = new Resource(method, path); // упаковываем в Resource

                //Проверяем, зарегистрирован ли ресурс вообще, и если нет - то шлём 404
                if (!resourceSet.contains(resource)) {
                    badRequest(out);
                    Thread.currentThread().interrupt();
                }

                //Проверяем, есть ли зарегистрированный Handler для ресурса, и если нет - то шлём 404
                if (handlers.get(resource) == null) {
                    badRequest(out);
                    Thread.currentThread().interrupt();
                }

                //В этой точке кода ресурс есть и обработчик зарегистрирован, вызываем его метод
                handlers.get(resource).handle(request, out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

