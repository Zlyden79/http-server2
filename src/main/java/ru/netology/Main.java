package ru.netology;

import java.io.BufferedOutputStream;
import java.util.List;

public class Main {
  public static void main(String[] args) {
    Server server = new Server();

    Handler common = new CommonHandler();
    Handler classic = new ClassicHandler();

    List<String> validPaths = server.getValidPaths();
    //регистрируем обработчики для методов GET и POST
    for (String current : validPaths) {
      if ("/classic.html".equals(current)) {
        server.addHandler("GET", current, classic);
        server.addHandler("POST", current, classic);
      } else {
        server.addHandler( "GET", current, common);
        server.addHandler( "POST", current, common);
      }
    }

    server.start();
  }
}