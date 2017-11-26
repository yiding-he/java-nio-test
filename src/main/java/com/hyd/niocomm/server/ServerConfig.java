package com.hyd.niocomm.server;

/**
 * @author yidin
 */
public class ServerConfig {

    public static final int DEFAULT_MAX_WORKER = 5;

    public static final int DEFAULT_PORT = 3333;

    private int port = DEFAULT_PORT;

    private int maxWorker = DEFAULT_MAX_WORKER;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getMaxWorker() {
        return maxWorker;
    }

    public void setMaxWorker(int maxWorker) {
        this.maxWorker = maxWorker;
    }
}
