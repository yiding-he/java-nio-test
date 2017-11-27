package com.hyd.niocomm.client;

import com.hyd.niocomm.Request;
import com.hyd.niocomm.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 请求线程的“等待休息室”
 *
 * @author yiding_he
 */
public class WaitRoom {

    private static final Logger LOG = LoggerFactory.getLogger(WaitRoom.class);

    private Map<Long, ResponseFutureTask> taskMap = new ConcurrentHashMap<>();

    /**
     * 令一个请求线程等待
     *
     * @param request 要等待的请求
     */
    public ResponseFutureTask waitForResponse(Request request) {
        ResponseFutureTask responseFuture = new ResponseFutureTask(request.getSequence());
        this.taskMap.put(request.getSequence(), responseFuture);
        return responseFuture;
    }

    /**
     * 设置一个请求的回应，同时唤醒该请求的执行线程
     *
     * @param response 回应
     */
    public void setProceed(Response response) {
        long sequence = response.getSequence();

        if (taskMap.containsKey(sequence)) {
            taskMap.get(sequence).setResponse(response);
        } else {
            // 如果请求线程在等待一段时间后超时返回，则对应的请求回应将被抛弃
            LOG.error("Response abandoned: " + response.getSequence());
        }
    }

    /**
     * 清除指定的正在等待的请求
     *
     * @param sequence 请求序列
     */
    public void clear(long sequence) {
        taskMap.remove(sequence);
    }
}
