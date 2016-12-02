package org.stayfool.client.util;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

/**
 * Created by stayfool on 2016/12/2.
 * <p>
 * save values to channel
 */
public final class ChannelUtil {

    private static final AttributeKey<String> CLIENT_ID = AttributeKey.newInstance("CLIENT_ID");

    private ChannelUtil() {
    }

    public static void clientId(Channel channel, String clientId) {
        channel.attr(CLIENT_ID).set(clientId);
    }

    public static String clientId(Channel channel) {
        return channel.attr(CLIENT_ID).get();
    }
}
