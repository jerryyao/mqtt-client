package org.mqtt.client;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;

/**
 * Created by pactera on 2016/11/29.
 */
public class OutBufferTest {

    @Test
    public void outbuffer() {
        MqttClient client = new MqttClient(MqttClientOption.instance().host("localhost").port(1883));
        client.connect();
        CountDownLatch latch = new CountDownLatch(1);
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
