package com.hyd.niocomm.client;

import com.hyd.niocomm.Response;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;

/**
 * @author yiding_he
 */
public class SocketChannelClientHandler implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(SocketChannelClientHandler.class);

    private Selector selector;

    private boolean closed = false;

    /**
     * 执行写入的类
     */
    private SocketChannelReader<Response> socketChannelReader = new SocketChannelReader<>();

    /**
     * 执行读取的类
     */
    private SocketChannelWriter socketChannelWriter = new SocketChannelWriter();

    /**
     * 注册 selector 的队列。不能在外部线程直接注册 selector，那样会导致该线程挂起。注册只能在 selector
     * 线程来做，外部线程要做的就是：1）将要注册的 SocketChannel 放入这个 map；2）调用 selector 的 wakeup() 方法。
     */
    private final Map<SocketChannel, Integer> registerPendingMap = new ConcurrentHashMap<>();

    /**
     * 连接服务器的 SocketChannel
     */
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

    /**
     * 启动 selector 线程
     *
     * @param daemon 是否当进程退出时自动结束线程
     */
    public void start(boolean daemon) {
        Thread selectorThread = new Thread(() -> {
            try {
                start0();
            } catch (IOException e) {
                LOG.error("Error running selector", e);
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

        // process write queue
        this.socketChannelWriter.writeRequest();

        // process register queue
        Map<SocketChannel, Integer> pendingMap;
        synchronized (this.registerPendingMap) {
            pendingMap = new HashMap<>(this.registerPendingMap);
            this.registerPendingMap.clear();
        }

        pendingMap.forEach(((socketChannel, interest) -> {
            try {
                socketChannel.register(this.selector, interest);
                LOG.info("SocketChannel registered.");
            } catch (ClosedChannelException e) {
                LOG.error("", e);
            }
        }));
    }

    public synchronized SocketChannel openSocketChannel(Address address) throws IOException {

        if (this.clientSocketChannel != null && this.clientSocketChannel.isOpen()) {
            return this.clientSocketChannel;
        }

        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);

        this.registerPendingMap.put(socketChannel, OP_CONNECT | OP_READ);
        this.selector.wakeup();

        LOG.info("Connecting to " + address + "...");
        socketChannel.connect(new InetSocketAddress(address.getHost(), address.getPort()));

        while (!socketChannel.finishConnect()) {

        }
        LOG.info("Connected.");

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
