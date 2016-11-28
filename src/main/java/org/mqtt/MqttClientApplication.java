package org.mqtt;

import io.netty.util.CharsetUtil;
import org.mqtt.client.MqttClient;
import org.mqtt.client.MqttClientFactory;
import org.mqtt.client.event.EventListener;
import org.mqtt.client.message.AbstractMessage.QOSType;
import org.mqtt.client.message.PublishMessage;
import org.mqtt.client.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
public class MqttClientApplication {

    private static volatile AtomicInteger statistics = new AtomicInteger(0);
    private static volatile Logger log = LoggerFactory.getLogger(MqttClientApplication.class);

    public static void main(String[] args) throws Exception {
        SpringApplication.run(MqttClientApplication.class, args);
        pressureTest();
        // functionalTest();
    }

    public static void pressureTest() {

        MqttClient subClient = new MqttClient();
        subClient.connect();
        subClient.subscribe(Config.publish_topic, QOSType.valueOf(Config.qos));

        int threadNum = Config.connection_num / Config.connection_num_per_thread;

        ExecutorService es = Executors.newFixedThreadPool(threadNum);
        for (int j = 0; j < threadNum; j++) {
            es.submit(() -> {
                for (int i = 0; i < Config.connection_num_per_thread; i++) {
                    MqttClient client = new MqttClient();
                    client.connect();
//					log.error("client connected : {}", statistics.incrementAndGet());
//					String message = Thread.currentThread().getName() + "_message_" + i;
//					client.publish(Config.publish_topic, Config.qos, message);
                }
            });
        }
    }

    public static void functionalTest() throws Exception {

        List<MqttClient> clientList = new ArrayList<>();

        int threadNum = Config.connection_num / Config.connection_num_per_thread;

        ExecutorService es = Executors.newFixedThreadPool(threadNum);
        CountDownLatch latch = new CountDownLatch(Config.connection_num);
        for (int j = 0; j < threadNum; j++) {
            es.submit(() -> {
                for (int i = 0; i < Config.connection_num_per_thread; i++) {
                    MqttClient client = MqttClientFactory.createShareBootClient();
                    client.connect();
                    clientList.add(client);
                    latch.countDown();
                    log.error("client : {} connected", client.getClientId());
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
                     * org.mqtt.client.event.EventListener#subscribeSuccess()
                     */
                    @Override
                    public void subscribeSuccess() {
                        log.error("subscribe success : [clientID:{},topic:{}]", client.getClientId(), randomTopic);
                    }

                    /*
                     * (non-Javadoc)
                     *
                     * @see
                     * org.mqtt.client.event.EventListener#messageArrive(org.
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
                client.publish(randomTopic, QOSType.LEAST_ONE, content);

                client.setListener(new EventListener() {

                    /*
                     * (non-Javadoc)
                     *
                     * @see
                     * org.mqtt.client.event.EventListener#subscribeSuccess()
                     */
                    @Override
                    public void publishSuccess() {
                        log.error("publish success : [clientID:{},topic:{},content:{}]", client.getClientId(),
                                randomTopic, content);
                    }

                });
            }

            if (currentTimes >= runtimes) {
                clientList.parallelStream().forEach(client -> client.close());
                System.exit(0);
            }

            currentTimes++;
            int waitTime = new Random().nextInt(10);

            Thread.sleep(waitTime * 1000);
        }
    }

    private static String randomTopic(String[] topic, boolean allowWildcard) {
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
}
