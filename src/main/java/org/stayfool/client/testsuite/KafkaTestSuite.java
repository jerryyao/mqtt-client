package org.stayfool.client.testsuite;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created on 2016/12/19.
 *
 * @author stayfool
 */
public class KafkaTestSuite {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    String data = "device.data";
    String cmd = "device.cmd";
    String event = "device.conn";
    String brokerAddress = "localhost:9092";
    Properties props;
    private volatile boolean complete = false;

    private static final ConcurrentMap<String, AtomicInteger> receiveMsgSum = new ConcurrentHashMap<>();

    private ExecutorService es;

    public KafkaTestSuite(ExecutorService es) {
        this.es = es;
    }

    public void consume() {

        initProps();

        Consumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList(data, cmd, event));

        logger.info("subscribe success : {} {} {}", data, cmd, event);

        es.execute(() -> {
            AtomicInteger at = new AtomicInteger(0);
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(1000);

                records.forEach(record -> {

                    if (at.get() >= PropUtil.getClientNum() * 11 + 3) {
                        logger.error("test pass");
                        complete = true;
                    }
                    logger.info("receive NO. {} message : {}", at.incrementAndGet(), record.value());
                });
                if (complete)
                    break;
            }
        });
    }

    public void initProps() {
        props = new Properties();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "intigrationtest");
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, PropUtil.getKafkaAddress() + ":" + PropUtil.getKafkaPort());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 100);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 15000);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    }

    public boolean isComplete() {
        return complete;
    }
}
