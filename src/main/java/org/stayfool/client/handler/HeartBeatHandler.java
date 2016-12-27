package org.stayfool.client.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stayfool.client.util.ChannelUtil;
import org.stayfool.client.util.FixHeaderUtil;

/**
 * Created by pactera on 2016/11/28.
 * <p>
 * 心跳处理器,一定时间内没有读写操作则发送心跳
 */
public class HeartBeatHandler extends ChannelDuplexHandler {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent && ((IdleStateEvent) evt).state() == IdleState.ALL_IDLE) {
            ctx.channel().writeAndFlush(new MqttMessage(FixHeaderUtil.from(MqttMessageType.PINGREQ)));
            log.debug("send heartbeat : {}", ChannelUtil.clientId(ctx.channel()));
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }
}
