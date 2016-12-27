package org.stayfool.mqtt.test;

import org.junit.Test;
import org.stayfool.client.MqttClient;
import org.stayfool.client.MqttOption;
import org.stayfool.client.event.EventKey;
import org.stayfool.client.event.EventType;
import org.stayfool.client.message.PublishMessage;
import org.stayfool.client.message.QOSType;
import org.stayfool.client.util.IDUtil;

import java.nio.ByteBuffer;

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
            ByteBuffer buffer = ((PublishMessage) msg).getPayload().duplicate();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            System.err.println(new String(bytes));
        });

        client.connect();
        client.subscribe(topic1, QOSType.LEAST_ONE);

        client.publish(topic2, QOSType.LEAST_ONE, false, "refactortest level 0");
        client.publish(topic3, QOSType.LEAST_ONE, false, "refactortest level 1");
        client.publish(topic4, QOSType.LEAST_ONE, false, "refactortest level 2");
        client.publish(topic5, QOSType.LEAST_ONE, false, "refactortest level 3");

        while (true) {

        }
    }
}
