package org.stayfool.client.testsuite;

import com.alibaba.fastjson.JSON;
import org.stayfool.client.MqttClient;
import org.stayfool.client.MqttOption;
import org.stayfool.client.event.EventKey;
import org.stayfool.client.event.EventType;
import org.stayfool.client.message.PublishMessage;
import org.stayfool.client.message.QOSType;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created on 2016/12/19.
 *
 * @author stayfool
 */
public class BrokerTestSuite {

    //d:{userId}:{productId}:{deviceType}:{sn}
    private String baseId = "d:1:1:wifi:";
    //{userId}:{productId}
    private String username = "1:1";
    //{accesstoken}:{accesspassword}
    private String password = "11111111111111111111111111111111:password";
    private String topic = "data/{userId}/{productId}/{deviceId}/wifistatus/json";
    private String cmd = "cmd/{userId}/{productId}/{deviceId}";

    private final AtomicInteger sn = new AtomicInteger(0);

    private final ConcurrentMap<String, String> clientDeviceMap = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, AtomicInteger> receiveMsgSum = new ConcurrentHashMap<>();

    private ExecutorService es;

    public BrokerTestSuite(ExecutorService es) {
        this.es = es;
    }

    public void starTest() {
        int clientNum = PropUtil.getClientNum();
        es = Executors.newFixedThreadPool(clientNum);
        CountDownLatch latch = new CountDownLatch(clientNum);
        while (clientNum-- > 0) {
            es.execute(() -> {
                MqttClient client = createDevice();
                latch.countDown();

                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                client.subscribe(generateCmdTopic(client), QOSType.LEAST_ONE);

                for (int i = 0; i < 10; i++) {
                    WifiStatus status = new WifiStatus();
                    status.setConnectedNum(i);
                    status.setStrength(Double.valueOf(Math.random() * 10).intValue());
                    client.publish(generateDataTopic(client), QOSType.LEAST_ONE, false, JSON.toJSONString(status));

                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                client.disconnect();
            });
        }

    }

    private MqttClient createDevice() {
        String clientId = generateClientId();
        MqttOption option = MqttOption.instance()
                .clientId(clientId).shareBoot(false)
                .port(PropUtil.getBrokerPort())
                .host(PropUtil.getBrokerAddress())
                .username(PropUtil.getUserId() + ":" + PropUtil.getProductId())
                .password(PropUtil.getAccessToken() + ":" + PropUtil.getTokenPassword());
        MqttClient client = new MqttClient(option);

        receiveMsgSum.putIfAbsent(clientId, new AtomicInteger(0));

        client.addCallback(new EventKey(EventType.MESSAGE_ARRIVE, clientId), (msg) -> {
            PublishMessage m = ((PublishMessage) msg);
            if (m.getTopicName().startsWith("rsp")) {
                ByteBuffer buffer = ((PublishMessage) msg).getPayload();
                buffer.flip();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                ClientAttr arrt = JSON.parseObject(new String(bytes), ClientAttr.class);
                clientDeviceMap.put(clientId, arrt.getId());
            } else {
                receiveMsgSum.get(clientId).incrementAndGet();
            }
        });
        client.connect();

        return client;
    }

    private String generateClientId() {
        StringBuilder builder = new StringBuilder();
        builder.append("d:");
        builder.append(PropUtil.getUserId()).append(":");
        builder.append(PropUtil.getProductId()).append(":");
        builder.append(PropUtil.getDeviceType()).append(":");
        builder.append(PropUtil.getSnBase()).append(String.format("%05d", sn.incrementAndGet()));
        return builder.toString();
    }

    private String generateCmdTopic(MqttClient client) {
        StringBuilder builder = new StringBuilder();
        builder.append("cmd/");
        builder.append(PropUtil.getUserId()).append("/");
        builder.append(PropUtil.getProductId()).append("/");
        builder.append(getDeviceId(client.getClientId()));
        return builder.toString();
    }

    private String generateDataTopic(MqttClient client) {
        StringBuilder builder = new StringBuilder();
        builder.append("data/");
        builder.append(PropUtil.getUserId()).append("/");
        builder.append(PropUtil.getProductId()).append("/");
        builder.append(getDeviceId(client.getClientId())).append("/");
        builder.append("wifistatus/json");
        return builder.toString();
    }

    private String getDeviceId(String clientId) {
        while (!clientDeviceMap.containsKey(clientId)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return clientDeviceMap.get(clientId);
    }

    public String randomExistsDevice() {
        while (clientDeviceMap.isEmpty()) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        List<String> devices = new ArrayList<>(clientDeviceMap.values());

        return devices.get(Double.valueOf(Math.random() % devices.size()).intValue());
    }

}

class ClientAttr {

    /**
     * 客户端类型
     * 1:device, 2:application
     */
    private int type;
    /**
     * 客户端Id，对于设备，表示deviceId，对于应用表示UserToken
     */
    private String id;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

class WifiStatus {

    private int connectedNum;
    private int strength;

    public int getConnectedNum() {
        return connectedNum;
    }

    public void setConnectedNum(int connectedNum) {
        this.connectedNum = connectedNum;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }
}
