package com.hyd.niocomm.server;

import com.hyd.niocomm.Request;
import com.hyd.niocomm.nio.SocketChannelReader;
import com.hyd.niocomm.nio.SocketChannelWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;

/**
 * @author yidin
 */
public class SocketChannelServerHandler implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(SocketChannelServerHandler.class);

    private final ServerConfig serverConfig;

    private boolean running;

    private SocketChannelReader<Request> socketChannelReader = new SocketChannelReader<>();

    private SocketChannelWriter socketChannelWriter = new SocketChannelWriter();

    private Selector selector;

    ///////////////////////////////////////////////

    public SocketChannelServerHandler(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    ///////////////////////////////////////////////

    public SocketChannelReader<Request> getSocketChannelReader() {
        return socketChannelReader;
    }

    public SocketChannelWriter getSocketChannelWriter() {
        return socketChannelWriter;
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
                            SocketChannel client = server.accept();
                            client.configureBlocking(false);
                            client.socket().setTcpNoDelay(true);
                            client.register(selector, SelectionKey.OP_READ);

                            LOG.debug("Incoming connection from "+
                                    client.getRemoteAddress()
                                    +" accepted.");
                        }

                        if (key.isReadable()) {
                            this.socketChannelReader.readFromKey(key);
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

    private void writeResponses() {
        this.socketChannelWriter.writeResponse();
    }

    @Override
    public void close() throws IOException {
        this.running = false;
        this.selector.wakeup();
    }

    public void push(RequestContext requestContext) {
        this.socketChannelWriter.push(requestContext);
        this.selector.wakeup();
    }
}
