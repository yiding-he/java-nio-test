package com.hyd.niocomm.client;

import com.hyd.niocomm.Response;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yiding_he
 */
public class WaitRoom {

    private Map<Long, ResponseFutureTask> taskMap = new ConcurrentHashMap<>();

    public void setWait(ResponseFutureTask future) {
        this.taskMap.put(future.getSequence(), future);
    }

    public void setProceed(Response response) {
        long sequence = response.getSequence();
        if (taskMap.containsKey(sequence)) {
            taskMap.get(sequence).setResponse(response);
        }
    }

    public void clear(long sequence) {
        taskMap.remove(sequence);
    }
}
