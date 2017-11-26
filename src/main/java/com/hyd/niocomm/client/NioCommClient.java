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
 * @author yidin
 */
public class NioCommClient implements Closeable {

    private SocketChannelClientHandler socketChannelClientHandler;

    private WaitRoom waitRoom = new WaitRoom();

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

    private void initComponents() {

        if (this.socketChannelClientHandler != null) {
            return;
        }

        this.socketChannelClientHandler = new SocketChannelClientHandler();

        SocketChannelReader<Response> socketChannelReader = this.socketChannelClientHandler.getSocketChannelReader();
        socketChannelReader.setDecoder(ByteBufferEncoder::decodeResponse);
        socketChannelReader.setOnMessageDecoded(this::onMessageDecoded);

        this.socketChannelClientHandler.start(this.clientConfig.isDaemon());
    }

    private void onMessageDecoded(SocketChannel socketChannel, Response response) {
        this.waitRoom.setProceed(response);
    }

    public Response call(Request request) throws ClientException {

        request.setSequence(Id.next());
        ResponseFutureTask responseFuture = null;

        try {
            this.pushRequest(request);

            responseFuture = new ResponseFutureTask(request.getSequence());
            this.waitRoom.setWait(responseFuture);
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
        SocketChannel socketChannel = socketChannelClientHandler
                .openSocketChannel(clientConfig.pickAddress());


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
