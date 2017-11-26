package com.hyd.niocomm.client;

import com.hyd.niocomm.SocketChannelWrapper;

import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author yidin
 */
public class SocketChannelQueue<T> {

    private ConcurrentLinkedQueue<SocketChannelWrapper<T>> queue = new ConcurrentLinkedQueue<>();

    public void offer(T t, SocketChannel socketChannel) {
        queue.offer(new SocketChannelWrapper<>(t, socketChannel));
    }

    public T poll() {
        SocketChannelWrapper<T> socketChannelWrapper = queue.poll();
        return socketChannelWrapper == null ? null : socketChannelWrapper.getValue();
    }

    ///////////////////////////////////////////////

}
