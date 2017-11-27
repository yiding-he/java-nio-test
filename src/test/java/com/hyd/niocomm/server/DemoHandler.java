package com.hyd.niocomm.server;

import com.hyd.niocomm.Request;
import com.hyd.niocomm.Response;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author yidin
 */
public class DemoHandler implements Handler {

    @Override
    public Response handle(Request request) throws Exception {
        Integer delaySeconds = request.getParam("delaySeconds");

        Thread.sleep(delaySeconds * 1000);

        return new Response()
                .set("now", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
                .set("delaySeconds", delaySeconds);
    }
}
