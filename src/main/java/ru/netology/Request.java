package ru.netology;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.nio.charset.Charset;
import java.util.*;

public class Request {
    private String path;
    private String rawQueryString;
    private Set<String> queryParamNames;
    private String fragment;
    private String method;
    private String version;
    private Map<String, String> headers;
    private byte[] body;

    public Request() {
        headers = new HashMap<String, String>();
        queryParamNames = new HashSet<>();
    }

    public char[] byteToChar(byte[] array) {
        int arrLength = array.length;
        char[] charArr = new char[arrLength];
        for (int i=0; i < arrLength; i++){
            charArr[i] = (char) array[i];
        }
        return charArr;
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

    public String getRawQueryString() {
        return rawQueryString;
    }

    public void setRawQueryString(String rawQueryString) {
        this.rawQueryString = rawQueryString;
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
        return URLEncodedUtils.parse(this.rawQueryString, charset);
    }

    public Set<String> getQueryParamNames() {
        return queryParamNames;
    }

    public void addQueryParamNames() {
        List<NameValuePair> nameValuePairList = this.getQueryParams();
        for (NameValuePair nameValuePair : nameValuePairList) {
            queryParamNames.add(nameValuePair.getName());
        }
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
                ", rawQueryString='" + rawQueryString + '\'' + "\n" +
                ", queryParamNames='" + queryParamNames + '\'' + "\n" +
                ", fragment='" + fragment + '\'' + "\n" +
                ", body=" + Arrays.toString(body) + "\n" +
                '}';
    }
}

