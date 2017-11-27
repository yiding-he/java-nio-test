package com.hyd.niocomm.client;

import com.hyd.niocomm.Id;
import com.hyd.niocomm.Request;
import com.hyd.niocomm.Response;
import com.hyd.niocomm.nio.ByteBufferEncoder;
import com.hyd.niocomm.nio.SocketChannelReader;
import com.hyd.niocomm.server.RequestContext;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 异步 nio 客户端的实现。在调用的时候仍然是同步的。异步客户端不管有多少并发请求，永远只有一个 Socket
 * 连接。其实现机制就是：用一个单独的线程来处理 Socket I/O。这样做的好处是不存在连接池机制，而同时也会
 * 带来成本，即空闲的时候也会占用一个线程。
 * <p>
 * 因为发送请求和读取回应都是异步的，因此请求和回应都必须带上一个唯一的流水号以便匹配。
 *
 * @author yidin
 */
public class NioCommClient implements Closeable {

    /**
     * 用于处理 SocketChannel 通信
     */
    private SocketChannelClientHandler socketChannelClientHandler;

    /**
     * 用于等待请求返回
     */
    private WaitRoom waitRoom = new WaitRoom();

    /**
     * 相关配置
     */
    private ClientConfig clientConfig;

    public NioCommClient(String serverHost, int serverPort) {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.addAddress(serverHost, serverPort);
        this.clientConfig = clientConfig;

        initComponents();
    }

    public NioCommClient(ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        initComponents();
    }

    /**
     * 初始化（只会执行一次）
     */
    private void initComponents() {

        if (this.socketChannelClientHandler != null) {
            return;
        }

        this.socketChannelClientHandler = new SocketChannelClientHandler();

        SocketChannelReader<Response> socketChannelReader = this.socketChannelClientHandler.getSocketChannelReader();
        socketChannelReader.setDecoder(ByteBufferEncoder::decodeResponse);  // 设置如何解包
        socketChannelReader.setOnMessageDecoded(this::onMessageDecoded);    // 设置解包完毕后如何处理包

        this.socketChannelClientHandler.start(this.clientConfig.isDaemon());
    }

    private void onMessageDecoded(SocketChannel socketChannel, Response response) {
        this.waitRoom.setProceed(response);
    }

    /**
     * 发送请求并等待回应
     *
     * @param request 请求
     *
     * @return 回应
     *
     * @throws ClientException 如果发送请求或读取回应失败
     */
    public Response call(Request request) throws ClientException {

        request.setSequence(Id.next());
        ResponseFutureTask responseFuture = null;

        try {
            // 1、将请求送入发送队列；
            // 2、等待请求获得回应；
            // 3、返回请求的回应。
            this.pushRequest(request);
            responseFuture = this.waitRoom.waitForResponse(request);
            return responseFuture.get(this.clientConfig.getTimeoutMillis(), TimeUnit.MILLISECONDS);

        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new ClientException(e);
        } finally {
            if (responseFuture != null) {
                this.waitRoom.clear(responseFuture.getSequence());
            }
        }
    }

    private void pushRequest(Request request) throws IOException {

        // 如果尚未连接到服务器，则会创建一个连接
        SocketChannel socketChannel =
                socketChannelClientHandler.openSocketChannel(clientConfig.pickAddress());

        RequestContext requestContext = new RequestContext();
        requestContext.setSocketChannel(socketChannel);
        requestContext.setRequest(request);

        this.socketChannelClientHandler.push(requestContext);
    }

    @Override
    public void close() throws IOException {
        this.socketChannelClientHandler.close();
    }
}
