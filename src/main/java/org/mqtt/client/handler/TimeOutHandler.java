package org.mqtt.client.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import org.mqtt.client.message.PingReqMessage;
import org.mqtt.client.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by pactera on 2016/11/28.
 * <p>
 * 心跳处理器,一定时间内没有读写操作则发送心跳
 */
public class TimeOutHandler extends ChannelDuplexHandler {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt == IdleStateEvent.ALL_IDLE_STATE_EVENT) {
            ctx.channel().writeAndFlush(new PingReqMessage());
            log.debug("send heartbeat : {}", ctx.channel().attr(Config.CLIENT_ID).get());
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }
}
