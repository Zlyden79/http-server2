package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
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

        public void send404(BufferedOutputStream responseStream) throws IOException {
            responseStream.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            responseStream.flush();
        }

        @Override
        public void run() {
            try (final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 final var out = new BufferedOutputStream(socket.getOutputStream())) {
                // read only request line for simplicity
                // must be in form GET /path HTTP/1.1
                //Считываем RequestLine
                final var requestLine = in.readLine();
                final var parts = requestLine.split(" ");
                if (parts.length != 3) {
                    //just close socket
                    //continue;
                    Thread.currentThread().interrupt();
                }
                //парсим RequestLine и загоняем в поля класса
                final Request request = new Request();
                request.setVersion(parts[2]);
                request.setPath(parts[1]);
                request.setMethod(parts[0]);

                //Считываем и запоминаем заголовки
                while (true) {
                    String readLine = in.readLine();
                    //если всосали пустую строку => заголовки закончились, выходим из цикла
                    if ("".equals(readLine)) break;
                    //парсим заголовок и добавляем куда надо
                    //заголовки идут по одному на строку и разделяются на ключ и значения по разделителю ':'
                    //прикол в том, что readLine.split(":"); не подходит, ибо например у заголовка Host МОЖЕТ
                    //и в нашем случае даже ЕСТЬ двоеточие в значении - localhost:9999
                    //поэтому применяем "пляску с бубном" - делим строку вручную по первому ':'
                    int splitIndex = readLine.indexOf(':');
                    String key = readLine.substring(0, splitIndex);
                    //splitIndex + 2 - отсекаем само двоеточие и пробел после него
                    String value = readLine.substring(splitIndex + 2);
                    request.addHeaders(key, value);
                }

                //Будет ли у запроса тело? Смотрим на заголовок Content-Length - не равен ли нулю.
                //Достаём из заголовка размер тела запроса
                final String contentLength = request.getHeaders().get("Content-Length");
                int cLen = 0;
                if (contentLength != null) cLen = Integer.parseInt(contentLength);
                //Если оно не равно нулю - читаем тело запроса посимвольно и записываем в поле класса
                if (cLen != 0) {
                    char[] body = new char[cLen];
                    for (int i = 0; i < cLen; i++) {
                        body[i] = (char) in.read();
                    }
                    request.setBody(body);
                }
                //Тут мы завершили формирование объекта класса Request, выведем ка его в консоль поглазеть
                System.out.println(request);

                String method = request.getMethod(); //получаем метод запроса
                String path = request.getPath(); // получаем адрес ресурса
                Resource resource = new Resource(method, path); // упаковываем в Resource

                //Проверяем, зарегистрирован ли ресурс вообще, и если нет - то шлём 404
                if (!resourceSet.contains(resource)) {
                    send404(out);
                    Thread.currentThread().interrupt();
                }

                //Проверяем, есть ли зарегистрированный Handler для ресурса, и если нет - то шлём 404
                if (handlers.get(resource) == null) {
                    send404(out);
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

