package org.stayfool.mqtt.test;

import org.junit.Test;
import org.stayfool.client.MqttClient;
import org.stayfool.client.MqttOption;
import org.stayfool.client.event.EventKey;
import org.stayfool.client.event.EventType;
import org.stayfool.client.message.PublishMessage;
import org.stayfool.client.message.QOSType;

import java.nio.ByteBuffer;

/**
 * Created by stayfool on 2016/12/7.
 */
public class SSLtest {

    @Test
    public void ssl() {
        System.setProperty("javax.net.debug", "all");

        MqttOption option = MqttOption.instance()
                .port(1883).host("broker.hivemq.com")
//                .port(8883).host("localhost")
//                .keyPass("keyPassword").keyPath("nettyserver.jks")
                .username("admin").password("admin");
        MqttClient client = new MqttClient(option);
        client.connect();

        client.addCallback(new EventKey(EventType.MESSAGE_ARRIVE, client.getClientId()), (msg) -> {
            ByteBuffer b = ((PublishMessage) msg).getPayload();
            byte[] bs = new byte[b.remaining()];
            b.get(bs);
            System.out.println(new String(bs));
        });
        client.subscribe("testtopic/#", QOSType.LEAST_ONE);

        while (true) {

        }
    }
}
