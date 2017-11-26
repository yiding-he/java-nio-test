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
        Request request = new Request();
        request.setPath("/time/now");
        Response response = client.call(request);
        System.out.println(response.getData().getString("now"));

        client.close();
    }
}