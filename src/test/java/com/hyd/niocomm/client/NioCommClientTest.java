package com.hyd.niocomm.client;

import com.hyd.niocomm.Request;
import com.hyd.niocomm.Response;

/**
 * @author yidin
 */
public class NioCommClientTest {

    public static void main(String[] args) throws Exception {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.addAddress("localhost", 3333);

        NioCommClient client = new NioCommClient(clientConfig);

        new Thread(() -> call(client, 1)).start();
        new Thread(() -> call(client, 2)).start();
        new Thread(() -> call(client, 3)).start();

        client.close();
    }

    private static void call(NioCommClient client, int delaySeconds) {
        Response response = client.call(new Request("/time/now").setParam("delaySeconds", delaySeconds));
        System.out.println(response.getData().getInteger("delaySeconds") + " responded.");
    }
}