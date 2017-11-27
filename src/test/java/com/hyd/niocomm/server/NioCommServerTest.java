package com.hyd.niocomm.server;

/**
 * @author yidin
 */
public class NioCommServerTest {

    public static void main(String[] args) {
        NioCommServer server = new NioCommServer(3333);
        server.setHandler("/time/now", new DemoHandler());
        server.start();
    }
}