package com.hyd.niocomm.client;

import com.hyd.niocomm.ByteBufferEncoder;
import com.hyd.niocomm.Id;
import com.hyd.niocomm.Request;
import com.hyd.niocomm.SocketChannelWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

/**
 * @author yiding_he
 */
public class RequestWriter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestWriter.class);

    private SocketSelector socketSelector;

    private Consumer<ResponseFutureTask> onRequestEnqueued;

    private Consumer<Long> onRequestError;

    public void setOnRequestError(Consumer<Long> onRequestError) {
        this.onRequestError = onRequestError;
    }

    public void setOnRequestEnqueued(Consumer<ResponseFutureTask> onRequestEnqueued) {
        this.onRequestEnqueued = onRequestEnqueued;
    }

    public void setSelector(SocketSelector selector) {
        this.socketSelector = selector;
    }

    public ResponseFutureTask push(SocketChannel socketChannel, Request request) throws ClientException {
        request.setSequence(Id.next());
        socketSelector.push(socketChannel, request);

        ResponseFutureTask future = new ResponseFutureTask(request.getSequence());
        this.onRequestEnqueued.accept(future);
        return future;
    }

    void writeRequest(SocketChannelWrapper<Request> wrapper) {

        SocketChannel socketChannel = wrapper.getSocketChannel();
        Request request = wrapper.getValue();
        LOG.info("Writing request: " + request.getPath());

        try {
            ByteBuffer byteBuffer = ByteBufferEncoder.fromRequest(request);
            byteBuffer.flip();
            while (byteBuffer.hasRemaining()) {
                socketChannel.write(byteBuffer);
            }
        } catch (IOException e) {
            LOG.error("Error writing request", e);
            if (this.onRequestError != null) {
                this.onRequestError.accept(request.getSequence());
            }
        }
    }
}
