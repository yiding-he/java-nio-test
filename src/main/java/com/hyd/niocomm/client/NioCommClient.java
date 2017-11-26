package com.hyd.niocomm.client;

import com.hyd.niocomm.Request;
import com.hyd.niocomm.Response;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author yidin
 */
public class NioCommClient implements Closeable {

    private SocketSelector socketSelector;

    private ResponseReader responseReader;

    private RequestWriter requestWriter;

    private WaitRoom waitRoom = new WaitRoom();

    private ClientConfig clientConfig;

    public NioCommClient(String serverHost, int serverPort) {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.addAddress(serverHost, serverPort);
        this.clientConfig = clientConfig;

        initSocketSelector();
    }

    public NioCommClient(ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        initSocketSelector();
    }

    private void initSocketSelector() {
        if (this.socketSelector != null) {
            return;
        }

        this.responseReader = new ResponseReader();
        this.requestWriter = new RequestWriter();

        this.socketSelector = new SocketSelector();
        this.socketSelector.start(this.clientConfig.isDaemon());
        this.socketSelector.setOnReadable(this.responseReader::readResponse);
        this.socketSelector.setOnWritable(this.requestWriter::writeRequest);

        this.requestWriter.setSelector(this.socketSelector);
        this.requestWriter.setOnRequestEnqueued(this.waitRoom::setWait);
        this.requestWriter.setOnRequestError(this.waitRoom::clear);

        this.responseReader.setOnResponseReady(this.waitRoom::setProceed);
    }

    public Response call(Request request) throws ClientException {

        ResponseFutureTask responseFuture = null;

        try {
            SocketChannel socketChannel = socketSelector.openSocketChannel(clientConfig.pickAddress());
            responseFuture = this.requestWriter.push(socketChannel, request);
            return responseFuture.get(this.clientConfig.getTimeoutMillis(), TimeUnit.MILLISECONDS);
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new ClientException(e);
        } finally {
            if (responseFuture != null) {
                this.waitRoom.clear(responseFuture.getSequence());
            }
        }
    }

    @Override
    public void close() throws IOException {
        this.socketSelector.close();
    }
}
