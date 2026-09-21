package eu.ill.webx.relay;


import eu.ill.webx.WebXHostConfiguration;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WebXSyncHost {

    private final WebXHost host;
    private final Lock lock = new ReentrantLock();
    private boolean terminated = false;

    public WebXSyncHost(WebXHostConfiguration hostConfiguration) {
        this.host = new WebXHost(hostConfiguration);
    }

    public WebXHost getHost() {
        return host;
    }

    public void lock() {
        lock.lock();
    }

    public boolean tryLock() {
        return lock.tryLock();
    }

    public void unlock() {
        lock.unlock();
    }

    public boolean isTerminated() {
        return terminated;
    }

    public void terminate() {
        this.terminated = true;
    }
}
