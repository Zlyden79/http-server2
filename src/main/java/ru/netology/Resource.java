package ru.netology;

import java.util.Objects;

//объект класса хранит пару метод-путь
public class Resource {
    private String method;
    private String path;

    public Resource(String method, String path) {
        this.method = method;
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Resource resource = (Resource) o;
        return Objects.equals(method, resource.method) && Objects.equals(path, resource.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, path);
    }
}
