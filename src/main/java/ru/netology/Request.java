package ru.netology;

import java.util.Map;

public class Request {
    private MethodName nameMethod;
    private Map<String, String> headers;
    private String path;

    private Body body;

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public Request(MethodName nameMethod, String path, Map<String, String> headers) {
        this.nameMethod = nameMethod;
        this.headers = headers;
        this.path = path;
    }

    public MethodName getNameMethod() {
        return nameMethod;
    }


    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getPath() {
        return path;
    }


}
