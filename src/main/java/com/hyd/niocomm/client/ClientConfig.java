package com.hyd.niocomm.client;

import java.util.List;
import java.util.Random;
import java.util.Vector;

/**
 * @author yidin
 */
public class ClientConfig {

    private final List<Address> onlineServers = new Vector<>();

    private final List<Address> offlineServers = new Vector<>();

    private boolean daemon;

    private int timeoutMillis = 120000;

    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public boolean isDaemon() {
        return daemon;
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public void addAddress(String host, int port) {
        Address address = new Address(host, port);
        if (!this.onlineServers.contains(address)) {
            this.onlineServers.add(address);
        }
    }

    public void reportOffline(Address address) {
        if (this.onlineServers.remove(address)) {
            this.offlineServers.add(address);
        }
    }

    public void reportOnline(Address address) {
        if (this.offlineServers.remove(address)) {
            this.onlineServers.add(address);
        }
    }

    private Random random = new Random();

    public Address pickAddress() {
        synchronized (onlineServers) {
            if (onlineServers.isEmpty()) {
                return null;
            } else {
                return onlineServers.get(random.nextInt(onlineServers.size()));
            }
        }
    }
}
