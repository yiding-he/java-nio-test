package com.hyd.niocomm.server;

import com.hyd.niocomm.ByteBufferEncoder;
import com.hyd.niocomm.Request;
import com.hyd.niocomm.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

/**
 * 从 SocketChannel 读取内容，并组装成 Request 对象
 * 注意：原则上，读取过程必须在 selector 线程中执行
 * https://stackoverflow.com/a/14710747/395196
 *
 * @author yidin
 */
public class RequestReader implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(RequestReader.class);

    private Consumer<RequestContext> onRequestReady;

    private ByteBuffer readBuffer = ByteBuffer.allocate(8192);

    private ByteArrayOutputStream bos = new ByteArrayOutputStream();

    public void setOnRequestReady(Consumer<RequestContext> onRequestReady) {
        this.onRequestReady = onRequestReady;
    }

    public void readData(SelectionKey key) {
        try {
            ByteBufferEncoder.readFromKey(key, readBuffer, bos,
                    bytes -> processPacket(bytes, (SocketChannel) key.channel()));
        } catch (IOException e) {
            LOG.error("Error reading request", e);
        }
    }

    private void processPacket(byte[] packetBytes, SocketChannel socketChannel) {
        Request request = ByteBufferEncoder.decodeRequest(packetBytes);

        if (request != null && this.onRequestReady != null) {
            LOG.info("Request received: " + request.getPath());

            RequestContext requestContext = new RequestContext();
            requestContext.setSocketChannel(socketChannel);
            requestContext.setRequest(request);

            this.onRequestReady.accept(requestContext);
        }
    }

    @Override
    public void close() throws IOException {
    }
}
