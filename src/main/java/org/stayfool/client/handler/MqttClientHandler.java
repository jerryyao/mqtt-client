package org.stayfool.client.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.UnsupportedMessageTypeException;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stayfool.client.event.EventKey;
import org.stayfool.client.event.EventManager;
import org.stayfool.client.event.EventType;
import org.stayfool.client.session.SessionManager;
import org.stayfool.client.util.ChannelUtil;
import org.stayfool.client.util.FixHeaderUtil;

/**
 * @author stayfool
 */
public class MqttClientHandler extends ChannelInboundHandlerAdapter {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        MqttMessage message = (MqttMessage) msg;
        try {
            switch (message.fixedHeader().messageType()) {
                case CONNACK:
                    processConnAck(ctx, (MqttConnAckMessage) message);
                    break;
                case PUBLISH:
                    processPublish(ctx, (MqttPublishMessage) message);
                    break;
                case PUBACK:
                    processPubAck(ctx, (MqttPubAckMessage) message);
                    break;
                case PUBREC:
                    processPubRec(ctx, (MqttPubAckMessage) message);
                    break;
                case PUBREL:
                    processPubRel(ctx, (MqttPubAckMessage) message);
                    break;
                case PUBCOMP:
                    processPubComp(ctx, (MqttPubAckMessage) message);
                    break;
                case SUBACK:
                    processSubAck(ctx, (MqttSubAckMessage) message);
                    break;
                case UNSUBACK:
                    processUnsbAck(ctx, (MqttUnsubAckMessage) message);
                    break;
                case PINGRESP:
                    processPingResp(ctx, message);
                    break;
                default:
                    throw new UnsupportedMessageTypeException("Unacceptable Message Type");
            }
        } catch (Exception ex) {
            ctx.fireExceptionCaught(ex);
        }
    }

    private void processConnAck(ChannelHandlerContext ctx, MqttConnAckMessage message) {

        if (message.variableHeader().connectReturnCode().equals(MqttConnectReturnCode.CONNECTION_ACCEPTED)) {
            EventManager.notify(new EventKey(EventType.CONNECT_SUCCESS, ChannelUtil.clientId(ctx.channel())), message);
            log.debug("{} connect success", ChannelUtil.clientId(ctx.channel()));
        } else {
            EventManager.notify(new EventKey(EventType.CONNECT_FAILURE, ChannelUtil.clientId(ctx.channel())), message);
            log.debug("{} connect failure", ChannelUtil.clientId(ctx.channel()));
        }
    }

    private void processPublish(ChannelHandlerContext ctx, MqttPublishMessage message) {

        EventManager.notify(new EventKey(EventType.MESSAGE_ARRIVE, ChannelUtil.clientId(ctx.channel())), message);

        MqttQoS qos = message.fixedHeader().qosLevel();

        if (qos.equals(MqttQoS.AT_LEAST_ONCE)) {
            MqttFixedHeader fixedHeader = FixHeaderUtil.from(MqttMessageType.PUBACK);
            MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(message.variableHeader().messageId());
            MqttPubAckMessage pubAck = new MqttPubAckMessage(fixedHeader, variableHeader);
            ctx.channel().writeAndFlush(pubAck);
        } else if (qos.equals(MqttQoS.EXACTLY_ONCE)) {
            MqttFixedHeader fixedHeader = FixHeaderUtil.from(MqttMessageType.PUBREC);
            MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(message.variableHeader().messageId());
            MqttPubAckMessage pubRec = new MqttPubAckMessage(fixedHeader, variableHeader);
            ctx.channel().writeAndFlush(pubRec);
        }

        if (message.fixedHeader().isRetain()) {
            SessionManager.getSession(ChannelUtil.clientId(ctx.channel())).retainMessage(message);
        }

        ByteBuf bb = message.payload().duplicate();
        byte[] msg = new byte[bb.readableBytes()];
        bb.readBytes(msg);
        log.debug("accept message : topic-{}; content-{}", message.variableHeader().topicName(), new String(msg, CharsetUtil.UTF_8));
    }

    private void processPubAck(ChannelHandlerContext ctx, MqttPubAckMessage message) {
        EventManager.notify(new EventKey(EventType.PUBLISH_SUCCESS, ChannelUtil.clientId(ctx.channel())), message);
        log.debug("publish success : {}", ChannelUtil.clientId(ctx.channel()));
    }

    private void processPubRec(ChannelHandlerContext ctx, MqttPubAckMessage message) {
        MqttFixedHeader fixedHeader = FixHeaderUtil.from(MqttMessageType.PUBREL);
        MqttPubAckMessage pubRel = new MqttPubAckMessage(fixedHeader, message.variableHeader());
        ctx.channel().writeAndFlush(pubRel);
    }

    private void processPubRel(ChannelHandlerContext ctx, MqttPubAckMessage message) {
        MqttFixedHeader fixedHeader = FixHeaderUtil.from(MqttMessageType.PUBCOMP);
        MqttPubAckMessage pubComp = new MqttPubAckMessage(fixedHeader, message.variableHeader());
        ctx.channel().writeAndFlush(pubComp);
    }

    private void processPubComp(ChannelHandlerContext ctx, MqttPubAckMessage message) {
        EventManager.notify(new EventKey(EventType.PUBLISH_SUCCESS, ChannelUtil.clientId(ctx.channel())), message);
        log.debug("publish success : {}", ChannelUtil.clientId(ctx.channel()));
    }

    private void processSubAck(ChannelHandlerContext ctx, MqttSubAckMessage message) {
        boolean success = true;
        if (message.payload().grantedQoSLevels().isEmpty())
            success = false;
        if (success) {
            for (int qos : message.payload().grantedQoSLevels()) {
                if (MqttQoS.FAILURE.equals(MqttQoS.valueOf(qos))) {
                    success = false;
                    break;
                }
            }
        }
        if (success) {
            EventManager.notify(new EventKey(EventType.SUBSCRIBE_SUCCESS, ChannelUtil.clientId(ctx.channel())), "");
            log.debug("subscribe success : {} ", ChannelUtil.clientId(ctx.channel()));
        } else {
            EventManager.notify(new EventKey(EventType.SUBSCRIBE_FAILURE, ChannelUtil.clientId(ctx.channel())), "");
            log.debug("subscribe failure : {} ", ChannelUtil.clientId(ctx.channel()));
        }
    }

    private void processUnsbAck(ChannelHandlerContext ctx, MqttUnsubAckMessage message) {
        EventManager.notify(new EventKey(EventType.UNSUBSCRIBE_SUCCESS, ChannelUtil.clientId(ctx.channel())), message);
        log.debug("unsubscribe success : {} ", ChannelUtil.clientId(ctx.channel()));
    }

    @SuppressWarnings("unused")
    private void processPingResp(ChannelHandlerContext ctx, MqttMessage message) {
//        log.debug("unsubscribe success : {} ", ChannelUtil.clientId(ctx.channel()));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        log.error("bad thing happened : ", cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.close();
        EventManager.notify(new EventKey(EventType.DIS_CONNECT, ChannelUtil.clientId(ctx.channel())), "tcp connection broken");
        log.error("lose connection : {}", ChannelUtil.clientId(ctx.channel()));
    }

}
