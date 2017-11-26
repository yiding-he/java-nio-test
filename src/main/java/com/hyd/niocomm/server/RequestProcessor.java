package com.hyd.niocomm.server;

import com.hyd.niocomm.Request;
import com.hyd.niocomm.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * @author yidin
 */
public class RequestProcessor implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(RequestProcessor.class);

    private Map<String, Handler> handlerMappings = new HashMap<>();

    private ExecutorService workerPool;

    private ServerConfig serverConfig;

    private Consumer<RequestContext> onResponseReady;

    public RequestProcessor(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
        this.workerPool = Executors.newFixedThreadPool(serverConfig.getMaxWorker());
    }

    @Override
    public void close() throws IOException {
        if (this.workerPool != null) {
            this.workerPool.shutdownNow();
        }
    }

    public void setHandler(String path, Handler handler) {
        this.handlerMappings.put(path, handler);
    }

    public void setOnResponseReady(Consumer<RequestContext> onResponseReady) {
        this.onResponseReady = onResponseReady;
    }

    public void processRequest(RequestContext requestContext) {
        Request request = requestContext.getRequest();
        String path = request.getPath();

        if (handlerMappings.containsKey(path)) {
            Handler handler = handlerMappings.get(path);
            if (handler != null) {
                this.workerPool.submit(() -> executeHandler(requestContext, handler));
            } else {
                LOG.warn("No handler for path " + path);
                if (this.onResponseReady != null) {
                    Response response = new Response();
                    response.setResultCode(-1);
                    response.setMessage("Unknown path");
                    response.setSequence(request.getSequence());
                    requestContext.setResponse(response);
                    this.onResponseReady.accept(requestContext);
                }
            }
        }
    }

    private void executeHandler(RequestContext requestContext, Handler handler) throws ServerException {
        try {
            Request request = requestContext.getRequest();

            // process request
            Response response = handler.handle(request);
            response.setSequence(request.getSequence());

            if (this.onResponseReady != null) {
                requestContext.setResponse(response);
                this.onResponseReady.accept(requestContext);
            }
        } catch (Exception e) {
            throw new ServerException(e);
        }
    }

}
