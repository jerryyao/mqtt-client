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
 * Created by stayfool on 2016/12/9.
 */
public class AppSimulate {

    //d:{userId}:{productId}:{deviceType}:{sn}
    private String clientId = "a:11111111111111111111111111111111";
    //{userId}:{productId}
    private String username = "11111111111111111111111111111111";
    //{accesstoken}:{accesspassword}
    private String password = "password";

    private String deviceId = "2AAE1D6A70E54B3A99BF9FEC52890A12";

    private String topic = "data/1/1/+/wifistatus/json";

    private String cmd = "cmd/1/1/" + deviceId;

    @Test
    public void tetss() {
        MqttOption option = MqttOption.instance()
                .clientId(clientId)
                .port(1883).host("localhost")
                .username(username).password(password);
        MqttClient client = new MqttClient(option);

        client.connect();

        client.addCallback(new EventKey(EventType.MESSAGE_ARRIVE, client.getClientId()), (msg) -> {
            ByteBuffer b = ((PublishMessage) msg).getPayload();
            byte[] bs = new byte[b.remaining()];
            b.get(bs);
            System.out.println(new String(bs));
        });
        client.subscribe(topic, QOSType.LEAST_ONE);

        client.publish(cmd, QOSType.LEAST_ONE, false, "shutdown");

        while (true) {

        }
    }
}
