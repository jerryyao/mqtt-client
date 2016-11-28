package org.mqtt;

import org.junit.Test;
import org.mqtt.client.MqttClient;
import org.mqtt.client.event.EventListener;
import org.mqtt.client.message.AbstractMessage.QOSType;
import org.mqtt.client.message.PublishMessage;

public class JunitTestCase {

    //	@Test
    public void testWildcard() {

        MqttClient client = new MqttClient();
        client.setListener(new EventListener() {

            /*
             * (non-Javadoc)
             *
             * @see
             * org.mqtt.client.event.EventListener#messageArrive(org.mqtt.client
             * .message.PublishMessage)
             */
            @Override
            public void messageArrive(PublishMessage msg) {
                byte[] bytes = new byte[msg.getPayload().remaining()];
                msg.getPayload().get(bytes);
                System.err.println(new String(bytes));
                // log.error("3333333333333");
            }

            /*
             * (non-Javadoc)
             *
             * @see org.mqtt.client.event.EventListener#connectSuccess()
             */
            @Override
            public void connectSuccess() {
                // log.error("1111111111");
            }

            /*
             * (non-Javadoc)
             *
             * @see org.mqtt.client.event.EventListener#publishSuccess()
             */
            @Override
            public void publishSuccess() {
                // log.error("2222222222222222");
            }

        });

        client.connect();

        // client.subscribe("boot/+", QOSType.LEAST_ONE.byteValue());
        //
        // client.publish("boot", QOSType.LEAST_ONE.byteValue(), "boot root
        // content");
        //
        // client.publish("boot/test", QOSType.LEAST_ONE.byteValue(), "boot sub
        // content");
        // client.publish("boot/spring", QOSType.LEAST_ONE.byteValue(), "boot
        // spring content");
        // client.publish("boot/spring/third", QOSType.LEAST_ONE.byteValue(),
        // "level 3");

        client.subscribe("boot/#", QOSType.LEAST_ONE);

        client.publish("boot", QOSType.LEAST_ONE, "boot first	 content");

        client.publish("boot/second", QOSType.LEAST_ONE, "boot	 second content");

        client.publish("boot/second/three", QOSType.LEAST_ONE, "boot third content");

        while (true) {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void heartbeat() {
        MqttClient c = new MqttClient();
        c.connect();

        while (true) {

        }
    }
}
