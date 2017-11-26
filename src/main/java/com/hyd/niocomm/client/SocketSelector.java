package com.hyd.niocomm.client;

import com.hyd.niocomm.Request;
import com.hyd.niocomm.SocketChannelWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;

/**
 * @author yiding_he
 */
public class SocketSelector implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(SocketSelector.class);

    private Selector selector;

    private boolean closed = false;

    private Consumer<SelectionKey> onReadable;

    private Consumer<SocketChannelWrapper<Request>> onWritable;

    private ConcurrentLinkedQueue<SocketChannelWrapper<Request>> requestQueue = new ConcurrentLinkedQueue<>();

    private ConcurrentLinkedQueue<SocketChannelWrapper<Integer>> registerQueue = new ConcurrentLinkedQueue<>();

    private SocketChannel clientSocketChannel;

    public SocketSelector() throws ClientException {
        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            throw new ClientException("Error opening selector", e);
        }
    }

    public void setOnWritable(Consumer<SocketChannelWrapper<Request>> onWritable) {
        this.onWritable = onWritable;
    }

    public void setOnReadable(Consumer<SelectionKey> onReadable) {
        this.onReadable = onReadable;
    }

    public void start(boolean daemon) {
        Thread selectorThread = new Thread(() -> {
            try {
                start0();
            } catch (IOException e) {
                LOG.error("Error running selector");
            }
        });
        selectorThread.setDaemon(daemon);
        selectorThread.start();
    }

    private void start0() throws IOException {
        while (!closed) {

            processQueues();
            selector.select();

            if (selector.selectedKeys().isEmpty()) {
                processQueues();
            }

            for (Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
                SelectionKey key = i.next();
                i.remove();

                if (key.isConnectable()) {
                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    socketChannel.finishConnect();
                    LOG.info("SocketChannel connected.");
                }

                if (key.isReadable()) {
                    if (this.onReadable != null) {  // 这个地方始终无法达到
                        this.onReadable.accept(key);
                    }
                }
            }
        }
    }

    private void processQueues() throws IOException {

        if (this.clientSocketChannel != null) {
            while (!this.clientSocketChannel.finishConnect()) {

            }

            if (this.onWritable != null) {
                SocketChannelWrapper<Request> wrapper;
                while ((wrapper = this.requestQueue.poll()) != null) {
                    this.onWritable.accept(wrapper);
                }
            }
        }

        SocketChannelWrapper<Integer> register;
        while ((register = this.registerQueue.poll()) != null) {
            SocketChannel socketChannel = register.getSocketChannel();
            try {
                socketChannel.register(this.selector, register.getValue());
                LOG.info("SocketChannel registered.");
            } catch (ClosedChannelException e) {
                LOG.error("", e);
            }
        }
    }

    public SocketChannel openSocketChannel(Address address) throws IOException {

        if (this.clientSocketChannel != null && this.clientSocketChannel.isOpen()) {
            return this.clientSocketChannel;
        }

        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);

        registerQueue.offer(new SocketChannelWrapper<>(OP_CONNECT | OP_READ, socketChannel));
        this.selector.wakeup();

        LOG.info("Connecting to " + address + "...");
        socketChannel.connect(new InetSocketAddress(address.getHost(), address.getPort()));

        this.clientSocketChannel = socketChannel;
        return this.clientSocketChannel;
    }

    public void push(SocketChannel socketChannel, Request request) {
        this.requestQueue.offer(new SocketChannelWrapper<>(request, socketChannel));
        this.selector.wakeup();
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
        this.selector.wakeup();
    }
}
