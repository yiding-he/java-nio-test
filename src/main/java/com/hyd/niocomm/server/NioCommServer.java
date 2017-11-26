package com.hyd.niocomm.server;

import com.hyd.niocomm.CloseableUtils;
import com.hyd.niocomm.RequestContext;

/**
 * @author yidin
 */
public class NioCommServer {

    private SocketAcceptor socketAcceptor;

    private RequestProcessor requestProcessor;

    private RequestReader requestReader = new RequestReader();

    private ResponseWriter responseWriter = new ResponseWriter();

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

        this.requestReader.setOnRequestReady(this::processRequest);

        this.socketAcceptor = new SocketAcceptor(serverConfig);
        this.socketAcceptor.setOnReadable(this.requestReader::readData);
        this.socketAcceptor.setOnWritable(this.responseWriter::writeData);

        this.requestProcessor = new RequestProcessor(serverConfig);
        this.requestProcessor.setOnResponseReady(this.responseWriter::push);
    }

    private void processRequest(RequestContext requestContext) {
        if (this.requestProcessor != null) {
            this.requestProcessor.processRequest(requestContext);
        }
    }

    public void setHandler(String path, Handler handler) {
        this.requestProcessor.setHandler(path, handler);
    }

    public void start() {
        if (this.socketAcceptor.isRunning()) {
            return;
        }

        this.socketAcceptor.start();
        this.responseWriter.setSocketAcceptor(this.socketAcceptor);
    }

    public void stop() {
        CloseableUtils.close(
                this.socketAcceptor,
                this.requestReader,
                this.requestProcessor,
                this.responseWriter
        );
    }

}
