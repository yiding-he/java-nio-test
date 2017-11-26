package com.hyd.niocomm.client;

import com.hyd.niocomm.ByteBufferEncoder;
import com.hyd.niocomm.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.function.Consumer;

/**
 * @author yiding_he
 */
public class ResponseReader {

    private static final Logger LOG = LoggerFactory.getLogger(ResponseReader.class);

    private Consumer<Response> onResponseReady;

    private ByteBuffer readBuffer = ByteBuffer.allocate(8192);

    private ByteArrayOutputStream bos = new ByteArrayOutputStream();

    public void setOnResponseReady(Consumer<Response> onResponseReady) {
        this.onResponseReady = onResponseReady;
    }

    public void readResponse(SelectionKey key) {
        try {
            ByteBufferEncoder.readFromKey(
                    key, this.readBuffer, this.bos, this::processPacket);
        } catch (IOException e) {
            LOG.error("Error reading response", e);
        }
    }

    private void processPacket(byte[] packetBytes) {
        Response response = ByteBufferEncoder.decodeResponse(packetBytes);

        if (response != null && this.onResponseReady != null) {
            LOG.info("Response received: code=" + response.getResultCode());
            this.onResponseReady.accept(response);
        }
    }
}
