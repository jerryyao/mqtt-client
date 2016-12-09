package org.stayfool.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stayfool.client.event.EventKey;
import org.stayfool.client.event.EventManager;
import org.stayfool.client.event.EventType;
import org.stayfool.client.message.*;
import org.stayfool.client.util.ChannelUtil;

/**
 * @author stayfool
 */
public class MqttClientHandler extends ChannelInboundHandlerAdapter {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        AbstractMessage message = (AbstractMessage) msg;
        try {
            switch (message.getMessageType()) {
                case MessageType.CONNACK:
                    processConnAck(ctx, (ConnAckMessage) message);
                    break;
                case MessageType.PUBLISH:
                    processPublish(ctx, (PublishMessage) message);
                    break;
                case MessageType.PUBACK:
                    processPubAck(ctx, (PubAckMessage) message);
                    break;
                case MessageType.PUBREC:
                    processPubRec(ctx, (PubRecMessage) message);
                    break;
                case MessageType.PUBREL:
                    processPubRel(ctx, (PubRelMessage) message);
                    break;
                case MessageType.PUBCOMP:
                    processPubComp(ctx, (PubCompMessage) message);
                    break;
                case MessageType.SUBACK:
                    processSubAck(ctx, (SubAckMessage) message);
                    break;
                case MessageType.UNSUBACK:
                    processUnsbAck(ctx, (UnsubAckMessage) message);
                    break;
                case MessageType.PINGRESP:
                    processPingResp(ctx, (PingRespMessage) message);
                    break;
                default:
                    throw new UnsupportedOperationException("Unacceptable SessionMessage Type");
            }
        } catch (Exception ex) {
            ctx.fireExceptionCaught(ex);
        }
    }

    private void processConnAck(ChannelHandlerContext ctx, ConnAckMessage message) {
        if (message.getReturnCode() == ConnAckMessage.CONNECTION_ACCEPTED) {
            EventManager.notify(new EventKey(EventType.CONNECT_SUCCESS, ChannelUtil.clientId(ctx.channel())), message);
            log.debug("{} connect success", ChannelUtil.clientId(ctx.channel()));
        } else {
            EventManager.notify(new EventKey(EventType.CONNECT_FAILURE, ChannelUtil.clientId(ctx.channel())), message);
            log.debug("{} connect failure", ChannelUtil.clientId(ctx.channel()));
        }
    }

    private void processPublish(ChannelHandlerContext ctx, PublishMessage message) {

        EventManager.notify(new EventKey(EventType.MESSAGE_ARRIVE, ChannelUtil.clientId(ctx.channel())), message);

        if (message.getQos().byteValue() > QOSType.MOST_ONE.byteValue()) {
            if (message.getQos() == QOSType.LEAST_ONE) {
                PubAckMessage pubAck = new PubAckMessage();
                pubAck.setMessageID(message.getMessageID());
                pubAck.setQos(message.getQos());
                ctx.channel().writeAndFlush(pubAck);
            } else {
                PubRecMessage pubRec = new PubRecMessage();
                pubRec.setMessageID(message.getMessageID());
                pubRec.setQos(message.getQos());
                ctx.channel().writeAndFlush(pubRec);
            }
        }

        byte[] msg = new byte[message.getPayload().remaining()];
        message.getPayload().get(msg);
        log.debug("accept message : topic-{}; content-{}", message.getTopicName(), new String(msg, CharsetUtil.UTF_8));
    }

    private void processPubAck(ChannelHandlerContext ctx, PubAckMessage message) {

        EventManager.notify(new EventKey(EventType.PUBLISH_SUCCESS, ChannelUtil.clientId(ctx.channel())), message);
        log.debug("publish success : {}", ChannelUtil.clientId(ctx.channel()));
    }

    private void processPubRec(ChannelHandlerContext ctx, PubRecMessage message) {
        PubRelMessage pubRel = new PubRelMessage();
        pubRel.setMessageID(message.getMessageID());
        pubRel.setQos(message.getQos());
        ctx.channel().writeAndFlush(pubRel);
    }

    private void processPubRel(ChannelHandlerContext ctx, PubRelMessage message) {
        PubCompMessage pubComp = new PubCompMessage();
        pubComp.setMessageID(message.getMessageID());
        pubComp.setQos(message.getQos());
        ctx.channel().writeAndFlush(pubComp);
    }

    private void processPubComp(ChannelHandlerContext ctx, PubCompMessage message) {
        EventManager.notify(new EventKey(EventType.PUBLISH_SUCCESS, ChannelUtil.clientId(ctx.channel())), message);
        log.debug("publish success : {}", ChannelUtil.clientId(ctx.channel()));
    }

    private void processSubAck(ChannelHandlerContext ctx, SubAckMessage message) {
        boolean success = true;
        if (message.types().isEmpty())
            success = false;
        if (success) {
            for (QOSType qosType : message.types()) {
                if (qosType.equals(QOSType.FAILURE)) {
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

    private void processUnsbAck(ChannelHandlerContext ctx, UnsubAckMessage message) {
        EventManager.notify(new EventKey(EventType.UNSUBSCRIBE_SUCCESS, ChannelUtil.clientId(ctx.channel())), message);
        log.debug("unsubscribe success : {} ", ChannelUtil.clientId(ctx.channel()));
    }

    @SuppressWarnings("unused")
    private void processPingResp(ChannelHandlerContext ctx, PingRespMessage message) {
//        log.debug("unsubscribe success : {} ", ChannelUtil.clientId(ctx.channel()));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        EventManager.notify(new EventKey(EventType.DIS_CONNECT, ChannelUtil.clientId(ctx.channel())), cause);
        log.error("bad thing happened : ", cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.close();
        EventManager.notify(new EventKey(EventType.DIS_CONNECT, ChannelUtil.clientId(ctx.channel())), "tcp connection broken");
        log.error("lose connection : {}", ChannelUtil.clientId(ctx.channel()));
    }

}
