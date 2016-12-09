package org.stayfool.client.session;

import io.netty.channel.Channel;

/**
 * Created by stayfool on 2016/12/6.
 */
public interface Session {

    String clientId();

    void clientId(String clientId);

    Channel channel();

    void channel(Channel channel);

    boolean isActive();

    void connect();

    void disConnect();

    void waitingForCommit(Integer id, Object msg);

//    void waitingForCommit(Integer id, Object msg, Integer timeout, Boolean reSend);

    void commit(Integer id);
}
