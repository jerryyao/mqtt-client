/*
 * Copyright (c) 2012-2015 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package org.mqtt.client.parser;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.MessageToByteEncoder;
import org.mqtt.client.message.AbstractMessage;
import org.mqtt.client.message.MessageType;

import java.util.HashMap;
import java.util.Map;

/**
 * @author andrea
 */
public class MQTTEncoder extends MessageToByteEncoder<AbstractMessage> {

    @SuppressWarnings("rawtypes")
    private Map<Byte, DemuxEncoder> m_encoderMap = new HashMap<Byte, DemuxEncoder>();

    public MQTTEncoder() {
        m_encoderMap.put(MessageType.CONNECT, new ConnectEncoder());
        m_encoderMap.put(MessageType.CONNACK, new ConnAckEncoder());
        m_encoderMap.put(MessageType.PUBLISH, new PublishEncoder());
        m_encoderMap.put(MessageType.PUBACK, new PubAckEncoder());
        m_encoderMap.put(MessageType.SUBSCRIBE, new SubscribeEncoder());
        m_encoderMap.put(MessageType.SUBACK, new SubAckEncoder());
        m_encoderMap.put(MessageType.UNSUBSCRIBE, new UnsubscribeEncoder());
        m_encoderMap.put(MessageType.DISCONNECT, new DisconnectEncoder());
        m_encoderMap.put(MessageType.PINGREQ, new PingReqEncoder());
        m_encoderMap.put(MessageType.PINGRESP, new PingRespEncoder());
        m_encoderMap.put(MessageType.UNSUBACK, new UnsubAckEncoder());
        m_encoderMap.put(MessageType.PUBCOMP, new PubCompEncoder());
        m_encoderMap.put(MessageType.PUBREC, new PubRecEncoder());
        m_encoderMap.put(MessageType.PUBREL, new PubRelEncoder());
    }

    @Override
    protected void encode(ChannelHandlerContext chc, AbstractMessage msg, ByteBuf bb) throws Exception {
        @SuppressWarnings("unchecked")
        DemuxEncoder<AbstractMessage> encoder = m_encoderMap.get(msg.getMessageType());
        if (encoder == null) {
            throw new CorruptedFrameException("Can't find any suitable decoder for message type: " + msg.getMessageType());
        }
        encoder.encode(chc, msg, bb);
    }
}
