package com.hyd.niocomm;

import com.hyd.niocomm.server.Handler;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author yidin
 */
public class DemoHandler implements Handler {

    @Override
    public Response handle(Request request) throws Exception {
        Response response = new Response();
        response.getData().put("now", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        return response;
    }
}
