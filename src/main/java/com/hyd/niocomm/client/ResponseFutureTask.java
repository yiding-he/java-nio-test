package com.hyd.niocomm.client;

import com.hyd.niocomm.Response;

import java.util.concurrent.FutureTask;

/**
 * @author yiding_he
 */
public class ResponseFutureTask extends FutureTask<Response> {

    private long sequence;

    public ResponseFutureTask(long sequence) {
        super(() -> null);
        this.sequence = sequence;
    }

    public long getSequence() {
        return sequence;
    }

    public void setResponse(Response response) {
        set(response);
    }

    @Override
    public void run() {
        throw new IllegalStateException("Call setResponse()");
    }
}
