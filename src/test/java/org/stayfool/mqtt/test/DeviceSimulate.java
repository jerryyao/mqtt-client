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
 * Created by stayfool on 2016/12/7.
 */
public class DeviceSimulate {

    //d:{userId}:{productId}:{deviceType}:{sn}
    private String clientId = "d:1:1:wifi:00001";
    //{userId}:{productId}
    private String username = "1:1";
    //{accesstoken}:{accesspassword}
    private String password = "11111111111111111111111111111111:password";

    private String deviceId = "2AAE1D6A70E54B3A99BF9FEC52890A12";

    private String topic = "data/1/1/" + deviceId + "/wifistatus/json";

    private String cmd = "cmd/1/1/" + deviceId;

    @Test
    public void ssl() throws InterruptedException {
//        System.setProperty("javax.net.debug", "all");

        MqttOption option = MqttOption.instance()
                .clientId(clientId)
                .port(1883).host("localhost")
//                .port(8883).host("localhost")
//                .keyPass("keyPassword").keyPath("nettyserver.jks")
                .username(username).password(password);
        MqttClient client = new MqttClient(option);

        client.addCallback(EventType.MESSAGE_ARRIVE, (msg) -> {
            ByteBuf b = ((MqttPublishMessage) msg).payload();
            System.out.println(b.toString(CharsetUtil.UTF_8));
        });

        client.connect();

        client.subscribe(cmd, MqttQoS.AT_LEAST_ONCE);

        while (true) {
//            client.publish(topic, MqttQoS.AT_LEAST_ONCE, false, "{\"id\":\"client connect\",\"content\":\"I am in!\"}");
            Thread.sleep(1000);
        }
    }
}
