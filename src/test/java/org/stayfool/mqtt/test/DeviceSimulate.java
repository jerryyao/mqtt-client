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
    public void ssl() {
//        System.setProperty("javax.net.debug", "all");

        MqttOption option = MqttOption.instance()
                .clientId(clientId)
                .port(1883).host("localhost")
//                .port(8883).host("localhost")
//                .keyPass("keyPassword").keyPath("nettyserver.jks")
                .username(username).password(password);
        MqttClient client = new MqttClient(option);

        client.addCallback(new EventKey(EventType.MESSAGE_ARRIVE, clientId), (msg) -> {
            ByteBuffer buffer = ((PublishMessage) msg).getPayload();
            byte[] bytes = new byte[buffer.remaining()];
            deviceId = new String(bytes);
            System.out.println(deviceId);
            client.removeCallback(new EventKey(EventType.MESSAGE_ARRIVE, clientId));
        });

        client.connect();

        client.addCallback(new EventKey(EventType.MESSAGE_ARRIVE, client.getClientId()), (msg) -> {
            ByteBuffer b = ((PublishMessage) msg).getPayload();
            byte[] bs = new byte[b.remaining()];
            b.get(bs);
            System.out.println(new String(bs));
        });

        client.subscribe(cmd,QOSType.LEAST_ONE);
        client.publish(topic, QOSType.LEAST_ONE, false, "{\"id\":\"client connect\",\"content\":\"I am in!\"}");

        while (true) {

        }
    }
}
