package org.stayfool.mqtt.test;

import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.CharsetUtil;
import org.junit.Test;
import org.stayfool.client.MqttClient;
import org.stayfool.client.MqttOption;
import org.stayfool.client.event.EventType;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created on 2016/12/28.
 *
 * @author stayfool
 */
public class MultiClientTest {

    //    @Test
    public void multiClient() {

        ExecutorService es = Executors.newCachedThreadPool();

        CountDownLatch latch = new CountDownLatch(1);

        for (int i = 0; i < 10; i++) {
            int j = i;
            es.execute(() -> {
                System.out.println(Thread.currentThread().getName() + " prepared ");
                try {
                    latch.await();
                } catch (InterruptedException e) {
                }
                MqttOption option = MqttOption.instance().host("iot.eclipse.org");
                MqttClient client = new MqttClient(option);
                client.connect();

                client.addCallback(EventType.MESSAGE_ARRIVE, msg -> {
                    MqttPublishMessage pms = (MqttPublishMessage) msg;
                    System.out.println(
                            Thread.currentThread().getName()
                                    + " receive : "
                                    + " topic : "
                                    + pms.variableHeader().topicName()
                                    + " content : "
                                    + pms.payload().toString(CharsetUtil.UTF_8));
                });
                client.subscribe("multi/client", MqttQoS.AT_LEAST_ONCE);
                client.publish("multi/client", MqttQoS.AT_LEAST_ONCE, false, Thread.currentThread().getName());

                System.out.println(Thread.currentThread().getName() + " started ");
            });
        }
        latch.countDown();

    }

    @Test
    public void sslTest() {
        MqttOption option = MqttOption.instance()
                .host("iot.eclipse.org").port(8883)
                .keyPass("password").keyPath("classpath:clientkeystore.jks");

        MqttClient client = new MqttClient(option);
        client.connect();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        client.close();
    }
}
