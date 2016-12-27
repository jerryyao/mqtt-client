package org.stayfool.mqtt.test;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.CharsetUtil;
import org.junit.Test;
import org.stayfool.client.MqttClient;
import org.stayfool.client.MqttOption;
import org.stayfool.client.event.EventKey;
import org.stayfool.client.event.EventType;
import org.stayfool.client.util.IDUtil;

/**
 * Created on 2016/12/26.
 *
 * @author stayfool
 */
public class RefactorTest {

    String topic = "data/1/1/+/wifistatus/json";
    String topic1 = "data/1/1/#";
    String topic2 = "data/1/1/2AAE1D6A70E54B3A99BF9FEC52890A12/wifistatus/json";
    String topic3 = "data/1/1/2AAE1D6A70E54B3A99BF9FEC52890A12/wifistatus/json/level1";
    String topic4 = "data/1/1/asdfasdf/wifistatus/json";
    String topic5 = "data/1/1/2AAE1D6A70E54B3A99BF9FEC52890A12/wifistatus/json/level1/level2";

    @Test
    public void refactorTest() {
        MqttOption option = MqttOption.instance()
                .host("localhost")
                .port(1883)
                .clientId(IDUtil.uuid());

        MqttClient client = new MqttClient(option);

        client.addCallback(new EventKey(EventType.MESSAGE_ARRIVE, option.clientId()), (msg) -> {
            ByteBuf b = ((MqttPublishMessage) msg).payload();
            System.out.println(b.toString(CharsetUtil.UTF_8));
        });

        client.connect();
        client.subscribe(topic1, MqttQoS.AT_LEAST_ONCE);

        client.publish(topic2, MqttQoS.AT_LEAST_ONCE, false, "refactortest level 0");
        client.publish(topic3, MqttQoS.AT_LEAST_ONCE, false, "refactortest level 1");
        client.publish(topic4, MqttQoS.AT_LEAST_ONCE, false, "refactortest level 2");
        client.publish(topic5, MqttQoS.AT_LEAST_ONCE, false, "refactortest level 3");

        while (true) {

        }
    }
}
