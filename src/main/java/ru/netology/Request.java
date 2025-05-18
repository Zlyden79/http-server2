package ru.netology;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Request {
    private String path;
    private String queryString;
    private String fragment;
    private String method;
    private String version;
    private Map<String, String> headers;
    private byte[] body;

    public Request() {
        headers = new HashMap<String, String>();
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void addHeaders(String key, String value) {
        headers.put(key, value);
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public String getFragment() {
        return fragment;
    }

    public void setFragment(String fragment) {
        this.fragment = fragment;
    }

    public List<NameValuePair> getQueryParams() {
        Charset charset = Charset.forName("UTF-8");
        return URLEncodedUtils.parse(this.queryString, charset);
    }

    public List<NameValuePair> getQueryParam(String name) {
        return getQueryParams().stream()
                .filter(a -> a.getName().equals(name))
                .toList();
    }

    @Override
    public String toString() {
        return "Request{ \n" +
                "method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", version='" + version + '\'' + "\n" +
                ", headers=" + headers + "\n" +
                ", queryString='" + queryString + '\'' + "\n" +
                ", fragment='" + fragment + '\'' + "\n" +
                ", body=" + Arrays.toString(body) + "\n" +
        '}';
    }
}

