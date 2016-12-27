package org.stayfool.client.util;

import io.netty.handler.codec.UnsupportedMessageTypeException;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;

/**
 * Created on 2016/12/27.
 *
 * @author stayfool
 */
public final class FixHeaderUtil {
    private FixHeaderUtil() {

    }

    /**
     * 生成 FixHeader
     * 不支持 {@link MqttMessageType#PUBLISH}
     *
     * @param mt {@link MqttMessageType}
     * @return {@link MqttFixedHeader}
     */
    public static MqttFixedHeader from(MqttMessageType mt) {
        MqttQoS qos = MqttQoS.AT_MOST_ONCE;
        boolean isDup = false;
        boolean isRetain = false;
        int remainingLength = 0;

        switch (mt) {
            case PUBREL:
            case SUBSCRIBE:
            case UNSUBSCRIBE:
                qos = MqttQoS.AT_LEAST_ONCE;
                break;
            case PUBLISH:
                throw new UnsupportedMessageTypeException("unsupport publish message type");
            default:
                break;
        }

        return new MqttFixedHeader(mt, isDup, qos, isRetain, remainingLength);
    }
}
