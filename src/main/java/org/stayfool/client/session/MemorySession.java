package org.stayfool.client.session;

import io.netty.channel.Channel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by stayfool on 2016/12/5.
 */
public class MemorySession implements Session {

    private boolean isConnect;
    private String clientId;
    private Channel channel;
    private ConcurrentMap<Integer, Object> waittingForCommit = new ConcurrentHashMap<>();

    public String clientId() {
        return clientId;
    }

    public void clientId(String clientId) {
        this.clientId = clientId;
    }

    public Channel channel() {
        return channel;
    }

    public void channel(Channel channel) {
        this.channel = channel;
    }

    public void connect() {
        isConnect = true;
    }

    public void disConnect() {
        isConnect = false;
    }

    public boolean isActive() {
        return isConnect;
    }

    public void waitingForCommit(Integer id, Object msg) {
        waittingForCommit.put(id, msg);
    }

    public void commit(Integer id) {
        waittingForCommit.remove(id);
    }

}
