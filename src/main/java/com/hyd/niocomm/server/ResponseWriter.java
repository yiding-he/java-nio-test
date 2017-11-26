package com.hyd.niocomm.server;

import com.hyd.niocomm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author yidin
 */
public class ResponseWriter implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(ResponseWriter.class);

    private SocketAcceptor socketAcceptor;

    public void setSocketAcceptor(SocketAcceptor socketAcceptor) {
        this.socketAcceptor = socketAcceptor;
    }

    @Override
    public void close() throws IOException {
    }

    public void push(RequestContext requestContext) {
        SocketChannel socketChannel = requestContext.getSocketChannel();
        this.socketAcceptor.push(socketChannel, requestContext.getResponse());
        requestContext.setRequest(null); // release some memory
    }

    // should be called in the main select thread.
    public void writeData(SocketChannelWrapper<Response> wrapper) {
        SocketChannel socketChannel = wrapper.getSocketChannel();
        Response response = wrapper.getValue();

        if (!socketChannel.isOpen()) {
            LOG.info("SocketChannel closed. Not writing.");
            return;
        }

        LOG.info("Writing response: " + response.getData());

        try {
            ByteBuffer byteBuffer = ByteBufferEncoder.encode(response);
            while (byteBuffer.hasRemaining()) {
                socketChannel.write(byteBuffer);
            }
        } catch (IOException e) {
            LOG.error("Error writing data", e);
            CloseableUtils.close(socketChannel);
        }
    }
}
