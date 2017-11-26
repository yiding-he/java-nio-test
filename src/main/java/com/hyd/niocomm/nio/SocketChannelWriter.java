package com.hyd.niocomm.nio;

import com.hyd.niocomm.CloseableUtils;
import com.hyd.niocomm.client.ClientException;
import com.hyd.niocomm.server.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Function;

/**
 * @author yidin
 */
public class SocketChannelWriter {

    private static final Logger LOG = LoggerFactory.getLogger(SocketChannelWriter.class);

    private final Queue<RequestContext> queue = new LinkedList<>();

    public synchronized void push(RequestContext requestContext) {
        if (!this.queue.offer(requestContext)) {
            throw new ClientException("Request not queued.");
        }
    }

    // should be called in the main select thread.
    public synchronized void writeResponse() {
        RequestContext requestContext;
        while ((requestContext = queue.poll()) != null) {
            LOG.info("Write response: " + requestContext.getResponse().getData());
            writeData(requestContext, c -> ByteBufferEncoder.fromResponse(c.getResponse()));
        }
    }

    // should be called in the main select thread.
    public synchronized void writeRequest() {
        RequestContext requestContext;
        while ((requestContext = queue.poll()) != null) {
            LOG.info("Write request: " + requestContext.getRequest().getPath());
            writeData(requestContext, c -> ByteBufferEncoder.fromRequest(c.getRequest()));
        }
    }

    private void writeData(
            RequestContext requestContext,
            Function<RequestContext, ByteBuffer> toByteBuffer) {

        SocketChannel socketChannel = requestContext.getSocketChannel();
        if (!socketChannel.isOpen()) {
            LOG.info("SocketChannel closed. Not writing.");
            return;
        }

        try {
            ByteBuffer byteBuffer = toByteBuffer.apply(requestContext);
            int size = byteBuffer.position();
            byteBuffer.flip();

            while (byteBuffer.hasRemaining()) {
                socketChannel.write(byteBuffer);
            }
            LOG.info("Write finished: " + size + " bytes.");
        } catch (IOException e) {
            LOG.error("Error writing data", e);
            CloseableUtils.close(socketChannel);
        }
    }

}
