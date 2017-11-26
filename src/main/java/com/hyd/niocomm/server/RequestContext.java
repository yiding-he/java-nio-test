package com.hyd.niocomm.server;

import com.hyd.niocomm.Request;
import com.hyd.niocomm.Response;
import com.hyd.niocomm.nio.ByteBufferEncoder;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author yidin
 */
public class RequestContext {

    private SocketChannel socketChannel;

    private Request request;

    private Response response;

    private ByteBuffer byteBuffer;

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public ByteBuffer responseToByteBuffer() {
        if (this.byteBuffer == null) {
            this.byteBuffer = ByteBufferEncoder.fromResponse(this.response);
        }

        return this.byteBuffer;
    }
}
