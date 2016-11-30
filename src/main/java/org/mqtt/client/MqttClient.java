package org.mqtt.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import org.mqtt.client.event.EventKey;
import org.mqtt.client.event.EventListener;
import org.mqtt.client.event.EventManager;
import org.mqtt.client.event.EventType;
import org.mqtt.client.handler.HeartBeatHandler;
import org.mqtt.client.handler.MqttClientHandler;
import org.mqtt.client.message.*;
import org.mqtt.client.parser.MQTTDecoder;
import org.mqtt.client.parser.MQTTEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.concurrent.CountDownLatch;

/**
 * Created by pactera on 2016/11/30.
 *
 * @author stayfool
 */
public class MqttClient {


    private Logger log = LoggerFactory.getLogger(getClass());

    private static volatile Bootstrap boot;
    private Channel channel;
    private boolean isConnect;
    private MqttClientOption option;

    public MqttClient(MqttClientOption option) {
        if (option == null || !option.validate())
            throw new IllegalArgumentException();
        this.option = option;
    }

    /**
     * connect to server
     */
    public void connect() {
        initBoot();
        initChannel();
        doConnect();
    }

    /**
     * subscribe a top
     *
     * @param topic
     * @param qos
     */
    public void subscribe(String topic, QOSType qos) {
        subscribe(new String[]{topic}, qos);
    }

    /**
     * subscribe topics
     *
     * @param filters
     * @param qos
     */
    public void subscribe(String[] filters, QOSType qos) {
        checkConnect();

        SubscribeMessage msg = new SubscribeMessage();
        msg.setMessageID(Long.valueOf(System.currentTimeMillis()).intValue());
        for (String topic : filters)
            msg.addSubscription(new SubscribeMessage.Couple(qos.byteValue(), topic));

        sendMessage(msg);
    }

    /**
     * publish a message to a topic
     *
     * @param topic
     * @param qos
     * @param retain
     * @param contect
     */
    public void publish(String topic, QOSType qos, boolean retain, String contect) {
        checkConnect();

        PublishMessage msg = new PublishMessage();
        msg.setMessageID(Long.valueOf(System.currentTimeMillis()).intValue());
        msg.setQos(qos);
        msg.setTopicName(topic);
        msg.setPayload(ByteBuffer.wrap(contect.getBytes(CharsetUtil.UTF_8)));
        msg.setRetainFlag(retain);

        sendMessage(msg);
    }

    /**
     * cancel a subscription
     *
     * @param topic
     */
    public void unsubscribe(String topic) {
        unsubscribe(new String[]{topic});
    }

    /**
     * cancel some subscription
     *
     * @param filters
     */
    public void unsubscribe(String[] filters) {
        checkConnect();

        UnsubscribeMessage msg = new UnsubscribeMessage();
        msg.setMessageID(Long.valueOf(System.currentTimeMillis()).intValue());
        for (String topic : filters) {
            msg.addTopicFilter(topic);
        }

        sendMessage(msg);
    }

    /**
     * disconnect with server
     */
    public void disconnect() {
        checkConnect();
        sendMessage(new DisconnectMessage());
    }

    /**
     * return the connection status
     *
     * @return
     */
    public boolean isConnect() {
        return isConnect;
    }

    /**
     * get clientId
     *
     * @return
     */
    public String getClientId() {
        return option.clientId();
    }

    /**
     * when something than the listener intrest happens , listener will be call
     *
     * @param listener
     */
    public void setListener(EventListener listener) {
        EventManager.register(new EventKey(EventType.MESSAGE_ARRIVE, option.clientId()),
                (msg) -> listener.messageArrive((PublishMessage) msg));
        EventManager.register(new EventKey(EventType.PUBLISH_SUCCESS, option.clientId()), (msg) -> listener.publishSuccess());
        EventManager.register(new EventKey(EventType.CONNECT_SUCCESS, option.clientId()), (msg) -> listener.connectSuccess());
        EventManager.register(new EventKey(EventType.SUBSCRIBE_SUCCESS, option.clientId()), (msg) -> listener.subscribeSuccess());
        EventManager.register(new EventKey(EventType.DIS_CONNECT, option.clientId()), (msg) -> listener.disconnect());
    }

    /**
     * remove listener
     */
    public void removeListener() {
        EventManager.unRegister(new EventKey(EventType.MESSAGE_ARRIVE, option.clientId()));
        EventManager.unRegister(new EventKey(EventType.PUBLISH_SUCCESS, option.clientId()));
        EventManager.unRegister(new EventKey(EventType.CONNECT_SUCCESS, option.clientId()));
        EventManager.unRegister(new EventKey(EventType.SUBSCRIBE_SUCCESS, option.clientId()));
        EventManager.unRegister(new EventKey(EventType.DIS_CONNECT, option.clientId()));
    }


    private void doConnect() {
        ConnectMessage msg = new ConnectMessage();
        msg.setProtocolVersion((byte) 4);
        msg.setClientID(option.clientId());
        msg.setCleanSession(option.cleanSession());
        msg.setKeepAlive(option.keepAlive()); // secs
        msg.setWillFlag(option.willFlag());

        if (option.willFlag()) {
            msg.setWillQos(option.willQos().byteValue());
            msg.setWillRetain(option.willRetain());
            msg.setWillTopic(option.willTopic());
            msg.setWillMessage(option.willMessage().getBytes(CharsetUtil.UTF_8));
        }

        sendMessage(msg);

        sync(new EventKey(EventType.CONNECT_SUCCESS, option.clientId()));

        isConnect = true;
    }

    private void initBoot() {

        if (boot != null && option.shareBoot())
            return;

        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            boot = new Bootstrap();
            boot.group(workerGroup);
            boot.channel(NioSocketChannel.class);
            boot.option(ChannelOption.SO_KEEPALIVE, true);
            boot.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addFirst(new HeartBeatHandler());
                    pipeline.addFirst(new IdleStateHandler(0, 0, option.keepAlive()));
                    pipeline.addLast(new MQTTDecoder());
                    pipeline.addLast(new MQTTEncoder());
                    pipeline.addLast(new MqttClientHandler());

                    // 如果配置了SSL相关信息，则加入SslHandler
                    initSSL(pipeline);
                }
            });
        } catch (Exception ex) {
            try {
                workerGroup.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                // 忽略
            }
            log.error("init bootstrap failed", ex);
            exit();
        }
    }

    private void initChannel() {
        try {
            channel = boot.connect(option.host(), option.port()).sync().channel();
            channel.attr(MqttClientOption.CLIENT_ID).set(option.clientId());
        } catch (InterruptedException e) {
            log.error("init channel failed", e);
            exit();
        }
    }

    private void initSSL(ChannelPipeline pipeline) {

        if (!option.useSSL())
            return;

        TrustManagerFactory tmf = null;
        KeyManagerFactory kmf = null;

        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(option.keyPath())) {

            KeyStore ks = KeyStore.getInstance(MqttClientOption.JKS);
            ks.load(in, option.keyPass().toCharArray());
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);

            if (!option.clientMode()) {
                kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(ks, option.keyPass().toCharArray());
            }

            SSLContext ssl = SSLContext.getInstance(MqttClientOption.SSL);
            ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            SSLEngine engine = ssl.createSSLEngine();
            engine.setUseClientMode(option.clientMode());
            pipeline.addFirst(new SslHandler(engine));
        } catch (Exception e) {
            log.error("init SSL failed, use normal mode", e);
        }
    }

    private void checkConnect() {
        assert isConnect;
    }

    private void sendMessage(AbstractMessage msg) {
        channel.writeAndFlush(msg);
    }

    private void sync(EventKey event) {
        final CountDownLatch connectLatch = new CountDownLatch(1);
        EventManager.register(event, (msg) -> connectLatch.countDown());
        try {
            connectLatch.await();
        } catch (InterruptedException e) {
            log.error("sync {} failed, cause : {} ", event, e);
        }
    }

    private void exit() {
        System.exit(-1);
    }
}
