package org.stayfool.client;

import io.netty.util.CharsetUtil;
import org.junit.Test;
import org.stayfool.client.event.EventListener;
import org.stayfool.client.message.PublishMessage;
import org.stayfool.client.message.QOSType;
import org.stayfool.client.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JunitTestCase {

    Logger log = LoggerFactory.getLogger(getClass());

    MqttClientOption option = MqttClientOption.instance().host("localhost").port(1883);


    @Test
    public void heartbeat() {
        MqttClient c = new MqttClient(option);
        c.connect();

        while (true) {

        }
    }

    public void pressureTest() {

        MqttClient subClient = new MqttClient(option);
        subClient.connect();
        subClient.subscribe(Config.publish_topic, QOSType.valueOf(Config.qos));

        int threadNum = Config.connection_num / Config.connection_num_per_thread;

        ExecutorService es = Executors.newFixedThreadPool(threadNum);
        for (int j = 0; j < threadNum; j++) {
            es.submit(() -> {
                for (int i = 0; i < Config.connection_num_per_thread; i++) {
                    MqttClient client = new MqttClient(option);
                    client.connect();
//					log.error("client connected : {}", statistics.incrementAndGet());
//					String message = Thread.currentThread().getName() + "_message_" + i;
//					client.publish(Config.publish_topic, Config.qos, message);
                }
            });
        }
    }

    public void functionalTest() throws Exception {

        List<MqttClient> clientList = new ArrayList<>();

        int threadNum = Config.connection_num / Config.connection_num_per_thread;

        ExecutorService es = Executors.newFixedThreadPool(threadNum);
        CountDownLatch latch = new CountDownLatch(Config.connection_num);
        for (int j = 0; j < threadNum; j++) {
            es.submit(() -> {
                for (int i = 0; i < Config.connection_num_per_thread; i++) {
                    MqttClient client = new MqttClient(option);
                    client.connect();
                    clientList.add(client);
                    latch.countDown();
//                    log.error("client : {} connected", client.getClientId());
                }
            });
        }

        latch.await();

        int runtimes = 100;
        int currentTimes = 0;
        int upbound = clientList.size() - 11;
        while (true) {
            int index = new Random(System.currentTimeMillis()).nextInt(upbound);
            index = index < 9 ? 9 : index;

            String[] topic = UUID.randomUUID().toString().split("-");

            // 抽取10个订阅客户端订阅随机topic
            for (int i = index; i > index - 10; i--) {
                MqttClient client = clientList.get(i);
                client.removeListener();
                String randomTopic = randomTopic(topic, true);
                client.subscribe(randomTopic, QOSType.LEAST_ONE);

                client.setListener(new EventListener() {

                    /*
                     * (non-Javadoc)
                     *
                     * @see
                     * EventListener#subscribeSuccess()
                     */
                    @Override
                    public void subscribeSuccess() {
                        log.error("subscribe success : [clientID:{},topic:{}]", client.getClientId(), randomTopic);
                    }

                    /*
                     * (non-Javadoc)
                     *
                     * @see
                     * EventListener#messageArrive(org.
                     * mqtt.client.message.PublishMessage)
                     */
                    @Override
                    public void messageArrive(PublishMessage msg) {
                        byte[] bytes = new byte[msg.getPayload().remaining()];
                        msg.getPayload().get(bytes);
                        String content = new String(bytes, CharsetUtil.UTF_8);
                        log.error("accept message : [clientID:{},topic:{},content:{}]", client.getClientId(),
                                randomTopic, content);
                    }

                });
            }

            // 抽取10个客户端推送随机topic消息
            for (int i = index; i < index + 10; i++) {
                MqttClient client = clientList.get(i);
                client.removeListener();
                String randomTopic = randomTopic(topic, false);
                String content = "current time : " + LocalDateTime.now().toString();
                client.publish(randomTopic, QOSType.LEAST_ONE, false, content);

                client.setListener(new EventListener() {

                    /*
                     * (non-Javadoc)
                     *
                     * @see
                     * EventListener#subscribeSuccess()
                     */
                    @Override
                    public void publishSuccess() {
                        log.error("publish success : [clientID:{},topic:{},content:{}]", client.getClientId(),
                                randomTopic, content);
                    }

                });
            }

            if (currentTimes >= runtimes) {
                clientList.parallelStream().forEach(client -> client.disconnect());
                System.exit(0);
            }

            currentTimes++;
            int waitTime = new Random().nextInt(10);

            Thread.sleep(waitTime * 1000);
        }
    }

    private String randomTopic(String[] topic, boolean allowWildcard) {
        int topicIndex = new Random(System.currentTimeMillis()).nextInt(topic.length - 1);
        topicIndex = topicIndex == 0 ? 1 : topicIndex;
        StringBuilder builder = new StringBuilder();
        for (int j = 0; j < topicIndex; j++) {
            builder.append(topic[j]).append("/");
        }

        if (allowWildcard) {
            topicIndex = new Random(System.currentTimeMillis()).nextInt(3);
            switch (topicIndex) {
                case 0:
                    builder.append("+");
                    break;
                case 1:
                    builder.append("#");
                    break;
                default:
                    builder.deleteCharAt(builder.length() - 1);
                    break;
            }
        } else {
            builder.deleteCharAt(builder.length() - 1);
        }

        return builder.toString();
    }

    // @Test
    public void duplicateConnect() {
        MqttClient client = new MqttClient(option);
        client.connect();
        client.connect();
    }

    //    @Test
    public void duplicateClientID() {

        try {
            MqttClient client1 = new MqttClient(option);
            MqttClient client2 = new MqttClient(option);

            client1.connect();
            client2.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // @Test
    public void wildcard() {
        MqttClient client = new MqttClient(option);
        client.setListener(new EventListener() {

            /*
             * (non-Javadoc)
             *
             * @see
             * EventListener#messageArrive(org.mqtt.client
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
             * @see EventListener#connectSuccess()
             */
            @Override
            public void connectSuccess() {
                // log.error("1111111111");
            }

            /*
             * (non-Javadoc)
             *
             * @see EventListener#publishSuccess()
             */
            @Override
            public void publishSuccess() {
                // log.error("2222222222222222");
            }

        });

        client.connect();

        client.subscribe("boot/+", QOSType.LEAST_ONE);

        client.publish("boot", QOSType.LEAST_ONE, false, "boot root content");

        client.publish("boot/test", QOSType.LEAST_ONE, false, "boot sub content");
        client.publish("boot/spring", QOSType.LEAST_ONE, false, "boot spring content");

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
