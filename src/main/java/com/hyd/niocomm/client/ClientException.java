package com.hyd.niocomm.client;

/**
 * @author yiding_he
 */
public class ClientException extends RuntimeException {

    public ClientException() {
    }

    public ClientException(String message) {
        super(message);
    }

    public ClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientException(Throwable cause) {
        super(cause);
    }
}
