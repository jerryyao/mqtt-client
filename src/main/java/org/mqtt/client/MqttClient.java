package org.mqtt.client;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.mqtt.client.event.EventKey;
import org.mqtt.client.event.EventListener;
import org.mqtt.client.event.EventManager;
import org.mqtt.client.event.EventType;
import org.mqtt.client.message.*;
import org.mqtt.client.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.util.CharsetUtil;

public class MqttClient {

    private Logger log = LoggerFactory.getLogger(getClass());

    private Bootstrap boot;
    private Channel channel;
    private boolean isConnect;
    private boolean bootShareable;
    private String host;
    private int port;
    private String clientId;

    public MqttClient() {
        this(Config.server_url, Config.server_port);
    }

    public MqttClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.clientId = UUID.randomUUID().toString();
    }

    public MqttClient connect() {
        connectTcp(host, port);
        checkActive();
        ConnectMessage connectMessage = new ConnectMessage();
        connectMessage.setProtocolVersion((byte) 4);
        connectMessage.setClientID(clientId);
        connectMessage.setCleanSession(true);
        connectMessage.setKeepAlive(Config.keep_alive); // secs
        connectMessage.setWillFlag(false);

        sendMessage(connectMessage);

        sync(new EventKey(EventType.CONNECT_SUCCESS, channel));

        isConnect = true;

        return this;
    }

    public void subscribe(String topic, AbstractMessage.QOSType qos) {
        if (topic == null || topic.length() == 0 || qos == null)
            return;
        subscribe(Arrays.asList(new SubscribeMessage.Couple(qos.byteValue(), topic)));
    }

    public void subscribe(List<SubscribeMessage.Couple> filters) {
        if (filters == null || filters.isEmpty())
            return;
        checkConnect();

        SubscribeMessage msg = new SubscribeMessage();
        msg.setRetainFlag(false);
        msg.setQos(AbstractMessage.QOSType.valueOf(Config.qos));
        filters.stream().forEach(couple -> msg.addSubscription(couple));
        sendMessage(msg);
    }

    public void unsubscribe(String topic) {
        if (topic == null || topic.length() == 0)
            return;

        unsubscribe(Arrays.asList(topic));
    }

    private void unsubscribe(List<String> filters) {
        if (filters == null || filters.isEmpty())
            return;

        checkConnect();

        UnsubscribeMessage msg = new UnsubscribeMessage();
        msg.setQos(AbstractMessage.QOSType.valueOf(Config.qos));
        filters.stream().forEach(t -> msg.addTopicFilter(t));
        sendMessage(msg);
    }

    public void publish(String topic, AbstractMessage.QOSType qos, String content) {
        if (topic == null || topic.length() == 0 || qos == null)
            return;
        checkConnect();
        PublishMessage msg = new PublishMessage();
        msg.setClientId(clientId);
        msg.setTopicName(topic);
        msg.setQos(qos);
        msg.setPayload(ByteBuffer.wrap(content.getBytes(CharsetUtil.UTF_8)));

        if (qos.byteValue() > AbstractMessage.QOSType.MOST_ONE.byteValue())
            msg.setMessageID(new Random(System.currentTimeMillis()).nextInt());
        sendMessage(msg);
    }

    public void hearBeat() {
        PingReqMessage m = new PingReqMessage();
        sendMessage(m);
    }

    public void disConnect() {
        checkConnect();
        sendMessage(new DisconnectMessage());
        log.debug("client disconnect : {}", clientId);
    }

    public void close() {
        disConnect();
        checkActive();
        try {
            channel.disconnect().sync();
            channel.close().sync();
            if (!bootShareable)
                boot.config().group().shutdownGracefully().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnect() {
        return isConnect;
    }

    private void sync(EventKey event) {
        final CountDownLatch connectLatch = new CountDownLatch(1);
        EventManager.register(event, (msg) -> connectLatch.countDown());
        try {
            connectLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void connectTcp(String host, int port) {
        boot = MqttClientFactory.createBoot(bootShareable);
        try {
            channel = boot.connect(host, port).sync().channel();
            channel.attr(Config.CLIENT_ID).set(clientId);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void checkActive() {
        if (channel == null || !channel.isActive())
            throw new RuntimeException("TCP connecttion not active");
    }

    private void checkConnect() {
        if (!isConnect) {
            throw new RuntimeException("MQTT connecttion not active");
        }
    }

    private void sendMessage(AbstractMessage msg) {
        try {
            channel.writeAndFlush(msg).sync();
        } catch (InterruptedException e) {
            log.error("send failed : {}-{}", clientId, msg.getMessageType());
            e.printStackTrace();
        }
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @return the clientId
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * @param clientId the clientId to set
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
        channel.attr(Config.CLIENT_ID).set(clientId);
    }

    /**
     * @param listener the listener to set
     */
    public void setListener(EventListener listener) {
        EventManager.register(new EventKey(EventType.MESSAGE_ARRIVE, channel),
                (msg) -> listener.messageArrive((PublishMessage) msg));
        EventManager.register(new EventKey(EventType.PUBLISH_SUCCESS, channel), (msg) -> listener.publishSuccess());
        EventManager.register(new EventKey(EventType.CONNECT_SUCCESS, channel), (msg) -> listener.connectSuccess());
    }

    public void removeListener() {
        EventManager.unRegister(new EventKey(EventType.MESSAGE_ARRIVE, channel));
        EventManager.unRegister(new EventKey(EventType.PUBLISH_SUCCESS, channel));
        EventManager.unRegister(new EventKey(EventType.CONNECT_SUCCESS, channel));
    }

    /**
     * @param bootShareable the bootShareable to set
     */
    void setBootShareable(boolean bootShareable) {
        this.bootShareable = bootShareable;
    }

}
