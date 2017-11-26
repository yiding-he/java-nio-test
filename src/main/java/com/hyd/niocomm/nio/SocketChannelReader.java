package com.hyd.niocomm.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author yidin
 */
public class SocketChannelReader<T> {

    private static final Logger LOG = LoggerFactory.getLogger(SocketChannelReader.class);

    private ByteBuffer readBuffer = ByteBuffer.allocate(4096);

    private ByteArrayOutputStream packetBuffer = new ByteArrayOutputStream();

    private BiConsumer<SocketChannel, T> onMessageDecoded;

    private Function<byte[], T> decoder;

    public void setDecoder(Function<byte[], T> decoder) {
        this.decoder = decoder;
    }

    public void setOnMessageDecoded(BiConsumer<SocketChannel, T> onMessageDecoded) {
        this.onMessageDecoded = onMessageDecoded;
    }

    public void readFromKey(SelectionKey key) throws IOException {

        SocketChannel channel = (SocketChannel) key.channel();

        ByteBufferEncoder.readFromKey(key, readBuffer, packetBuffer, bytes -> {
            T t = this.decoder.apply(bytes);
            LOG.info("A " + t.getClass().getSimpleName() + " message received.");
            this.onMessageDecoded.accept(channel, t);
        });
    }
}
