package com.hyd.niocomm.server;

import com.hyd.niocomm.Request;
import com.hyd.niocomm.Response;

/**
 * @author yidin
 */
public interface Handler {

    Response handle(Request request) throws Exception;
}
