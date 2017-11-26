package com.hyd.niocomm;

import java.nio.channels.SocketChannel;

/**
 * @author yidin
 */
public class SocketChannelWrapper<T> {

    private T value;

    private SocketChannel socketChannel;

    public SocketChannelWrapper(T value, SocketChannel socketChannel) {
        this.value = value;
        this.socketChannel = socketChannel;
    }

    public T getValue() {
        return value;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }
}
