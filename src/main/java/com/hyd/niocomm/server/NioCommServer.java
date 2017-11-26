package com.hyd.niocomm.server;

import com.hyd.niocomm.CloseableUtils;
import com.hyd.niocomm.Request;
import com.hyd.niocomm.nio.ByteBufferEncoder;

import java.nio.channels.SocketChannel;

/**
 * @author yidin
 */
public class NioCommServer {

    private SocketChannelServerHandler socketChannelServerHandler;

    private RequestProcessor requestProcessor;

    ///////////////////////////////////////////////

    public NioCommServer(int port) {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(port);
        initComponents(serverConfig);
    }

    public NioCommServer(ServerConfig serverConfig) {
        initComponents(serverConfig);
    }

    ///////////////////////////////////////////////

    private void initComponents(ServerConfig serverConfig) {

        this.socketChannelServerHandler = new SocketChannelServerHandler(serverConfig);
        this.requestProcessor = new RequestProcessor(serverConfig);

        this.socketChannelServerHandler.getSocketChannelReader()
                .setDecoder(ByteBufferEncoder::decodeRequest);

        this.socketChannelServerHandler.getSocketChannelReader()
                .setOnMessageDecoded(this::onMessageDecoded);

        this.requestProcessor.setOnResponseReady(
                this.socketChannelServerHandler::push);
    }

    public void setHandler(String path, Handler handler) {
        this.requestProcessor.setHandler(path, handler);
    }

    public void start() {
        if (this.socketChannelServerHandler.isRunning()) {
            return;
        }

        this.socketChannelServerHandler.start();
    }

    public void stop() {
        CloseableUtils.close(
                this.socketChannelServerHandler,
                this.requestProcessor
        );
    }

    private void onMessageDecoded(SocketChannel channel, Request request) {
        RequestContext requestContext = new RequestContext();
        requestContext.setSocketChannel(channel);
        requestContext.setRequest(request);
        this.requestProcessor.processRequest(requestContext);
    }
}
