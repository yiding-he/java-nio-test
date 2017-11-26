package com.hyd.niocomm.server;

import com.hyd.niocomm.Response;
import com.hyd.niocomm.SocketChannelWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * @author yidin
 */
public class SocketAcceptor implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(SocketAcceptor.class);

    private final ServerConfig serverConfig;

    private boolean running;

    private Consumer<SelectionKey> onReadable;

    private Consumer<SocketChannelWrapper<Response>> onWritable;

    private Selector selector;

    private Queue<SocketChannelWrapper<Response>> responseQueue = new ConcurrentLinkedQueue<>();

    ///////////////////////////////////////////////

    public SocketAcceptor(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    ///////////////////////////////////////////////

    public void setOnReadable(Consumer<SelectionKey> onReadable) {
        this.onReadable = onReadable;
    }

    public void setOnWritable(Consumer<SocketChannelWrapper<Response>> onWritable) {
        this.onWritable = onWritable;
    }

    public boolean isRunning() {
        return running;
    }

    public void start() {
        new Thread(this::start0).start();
    }

    private void start0() {

        if (this.running) {
            return;
        }

        selector = null;
        ServerSocketChannel server = null;

        try {
            int port = this.serverConfig.getPort();

            selector = Selector.open();
            server = ServerSocketChannel.open();
            server.socket().bind(new InetSocketAddress(port));
            server.configureBlocking(false);
            server.register(selector, SelectionKey.OP_ACCEPT);

            this.running = true;
            LOG.info("Server started at port " + port + "...");

            while (this.running) {
                try {

                    writeResponses();
                    selector.select();
                    if (selector.selectedKeys().isEmpty()) {
                        writeResponses();
                    }

                    for (Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
                        SelectionKey key = i.next();
                        i.remove();

                        if (key.isConnectable()) {
                            ((SocketChannel) key.channel()).finishConnect();
                        }

                        if (key.isAcceptable()) {
                            LOG.debug("Incoming connection accepted.");
                            SocketChannel client = server.accept();
                            client.configureBlocking(false);
                            client.socket().setTcpNoDelay(true);
                            client.register(selector, SelectionKey.OP_READ);
                        }

                        if (key.isReadable()) {
                            if (this.onReadable != null) {
                                this.onReadable.accept(key);
                            }
                        }
                    }
                } catch (CancelledKeyException e) {
                    LOG.error("Client disconnected.");
                }
            }
        } catch (Throwable e) {
            throw new ServerException("Server failure: ", e);
        } finally {
            try {
                if (selector != null) {
                    selector.close();
                }
                if (server != null) {
                    server.socket().close();
                    server.close();
                }
            } catch (Exception e) {
                // do nothing - server failed
            }
        }
    }

    public void push(SocketChannel socketChannel, Response response) {
        this.responseQueue.offer(new SocketChannelWrapper<>(response, socketChannel));
        this.selector.wakeup();
    }

    private void writeResponses() {
        if (this.onWritable != null) {
            SocketChannelWrapper<Response> socketChannelWrapper;
            while ((socketChannelWrapper = this.responseQueue.poll()) != null) {
                this.onWritable.accept(socketChannelWrapper);
            }
        }
    }

    @Override
    public void close() throws IOException {
        this.running = false;
        this.selector.wakeup();
    }
}
