package org.stayfool.mqtt.test;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.CharsetUtil;
import org.junit.Test;
import org.stayfool.client.MqttClient;
import org.stayfool.client.MqttOption;
import org.stayfool.client.event.EventType;

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
    public void tetss() throws InterruptedException {
        MqttOption option = MqttOption.instance()
                .clientId(clientId)
                .port(1883).host("localhost")
                .username(username).password(password);
        MqttClient client = new MqttClient(option);

        client.addCallback(EventType.MESSAGE_ARRIVE, (msg) -> {
            ByteBuf b = ((MqttPublishMessage) msg).payload();
            System.out.println(b.toString(CharsetUtil.UTF_8));
        });

        client.connect();
        client.subscribe(topic, MqttQoS.AT_LEAST_ONCE);

        while (true) {
            client.publish(cmd, MqttQoS.AT_LEAST_ONCE, false, "shutdown");

            Thread.sleep(1000);
        }
    }
}
