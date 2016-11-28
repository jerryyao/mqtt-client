package org.mqtt;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mqtt.client.MqttClient;
import org.mqtt.client.event.EventListener;
import org.mqtt.client.message.AbstractMessage.QOSType;
import org.mqtt.client.message.PublishMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MqttClientApplicationTests {

    private static Logger log = LoggerFactory.getLogger(MqttClientApplicationTests.class);

    // @Test
    public void duplicateConnect() {
        MqttClient client = new MqttClient();
        client.connect();
        client.connect();
    }

    @Test
    public void duplicateClientID() {

        try {
            MqttClient client1 = new MqttClient();
            MqttClient client2 = new MqttClient();
            client2.setClientId(client1.getClientId());

            client1.connect();
            client2.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // @Test
    public static void main(String[] arg) {
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
                log.error("3333333333333");
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

        client.subscribe("boot/+", QOSType.LEAST_ONE);

        client.publish("boot", QOSType.LEAST_ONE, "boot root content");

        client.publish("boot/test", QOSType.LEAST_ONE, "boot sub content");
        client.publish("boot/spring", QOSType.LEAST_ONE, "boot spring content");

        // client.subscribe("boot/#", QOSType.LEAST_ONE.byteValue());
        //
        // client.publish("boot", QOSType.LEAST_ONE.byteValue(), "boot first
        // content");
        //
        // client.publish("boot/second", QOSType.LEAST_ONE.byteValue(), "boot
        // second content");
        //
        // client.publish("boot/second/three", QOSType.LEAST_ONE.byteValue(),
        // "boot third content");
    }
}
