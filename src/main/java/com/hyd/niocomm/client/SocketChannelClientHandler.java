package com.hyd.niocomm.client;

import com.hyd.niocomm.Response;
import com.hyd.niocomm.SocketChannelWrapper;
import com.hyd.niocomm.nio.SocketChannelReader;
import com.hyd.niocomm.nio.SocketChannelWriter;
import com.hyd.niocomm.server.RequestContext;
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

import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;

/**
 * @author yiding_he
 */
public class SocketChannelClientHandler implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(SocketChannelClientHandler.class);

    private Selector selector;

    private boolean closed = false;

    private SocketChannelReader<Response> socketChannelReader = new SocketChannelReader<>();

    private SocketChannelWriter socketChannelWriter = new SocketChannelWriter();

    private ConcurrentLinkedQueue<SocketChannelWrapper<Integer>> registerQueue = new ConcurrentLinkedQueue<>();

    private SocketChannel clientSocketChannel;

    public SocketChannelClientHandler() throws ClientException {
        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            throw new ClientException("Error opening selector", e);
        }
    }

    public SocketChannelReader<Response> getSocketChannelReader() {
        return socketChannelReader;
    }

    public SocketChannelWriter getSocketChannelWriter() {
        return socketChannelWriter;
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
                    this.socketChannelReader.readFromKey(key);
                }
            }
        }
    }

    private void processQueues() throws IOException {

        this.socketChannelWriter.writeRequest();

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

    @Override
    public void close() throws IOException {
        this.closed = true;
        this.selector.wakeup();
    }

    public void push(RequestContext requestContext) {
        this.socketChannelWriter.push(requestContext);
        this.selector.wakeup();
    }
}
