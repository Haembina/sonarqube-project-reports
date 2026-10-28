package com.haembina.projectreports.api;

public final class ApiCallException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;
    private final transient byte[] body;

    public ApiCallException(int status, byte[] body) {
        super("A SonarQube web service answered " + status);
        this.status = status;
        this.body = body.clone();
    }

    public int status() {
        return status;
    }

    public byte[] body() {
        return body.clone();
    }
}
