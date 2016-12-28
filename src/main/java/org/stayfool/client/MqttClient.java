package org.stayfool.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stayfool.client.event.EventCallback;
import org.stayfool.client.event.EventKey;
import org.stayfool.client.event.EventManager;
import org.stayfool.client.event.EventType;
import org.stayfool.client.handler.HeartBeatHandler;
import org.stayfool.client.handler.MqttClientHandler;
import org.stayfool.client.session.SessionManager;
import org.stayfool.client.util.ChannelUtil;
import org.stayfool.client.util.FixHeaderUtil;
import org.stayfool.client.util.IDUtil;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by pactera on 2016/11/30.
 *
 * @author stayfool
 */
public class MqttClient {

    private Logger log = LoggerFactory.getLogger(getClass());

    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final AtomicInteger clientCount = new AtomicInteger(0);
    private static volatile Bootstrap shareBoot;
    private Bootstrap priBoot;
    private Channel channel;
    private boolean isConnect;
    private final MqttOption option;

    public MqttClient(MqttOption option) {
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
     * @param topic 主题
     * @param qos   qos
     */
    public void subscribe(String topic, MqttQoS qos) {
        subscribe(new String[]{topic}, qos);
    }

    /**
     * subscribe topics
     *
     * @param filters 主题表达式
     * @param qos     qos
     */
    public void subscribe(String[] filters, MqttQoS qos) {
        checkConnect();

        List<MqttTopicSubscription> topicList = new ArrayList<>();
        for (String topic : filters)
            topicList.add(new MqttTopicSubscription(topic, qos));

        MqttFixedHeader fixedHeader = FixHeaderUtil.from(MqttMessageType.SUBSCRIBE);
        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(IDUtil.nextAvailableId(option.clientId()));
        MqttSubscribePayload payload = new MqttSubscribePayload(topicList);
        MqttSubscribeMessage msg = new MqttSubscribeMessage(fixedHeader, variableHeader, payload);

        sendMessage(msg);

        SessionManager.getSession(option.clientId()).waitingConfirm(msg);
        EventManager.register(new EventKey(EventType.SUBSCRIBE_COMPLETE, option.clientId()), (message) -> {
            MqttSubAckMessage ack = (MqttSubAckMessage) message;
            SessionManager.getSession(option.clientId()).confirmMessage(ack.variableHeader().messageId());
        });
    }

    /**
     * publish a message to a topic
     *
     * @param topic   主题
     * @param qos     qos
     * @param retain  是否保存
     * @param contect 消息内容
     */
    public void publish(String topic, MqttQoS qos, boolean retain, String contect) {
        checkConnect();

        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.PUBLISH,
                false,
                qos,
                retain,
                0
        );
        MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(
                topic,
                IDUtil.nextAvailableId(option.clientId())
        );

        ByteBuf payload = Unpooled.wrappedBuffer(contect.getBytes(CharsetUtil.UTF_8));

        MqttPublishMessage msg = new MqttPublishMessage(fixedHeader, variableHeader, payload);

        sendMessage(msg);

        if (qos.value() > MqttQoS.AT_MOST_ONCE.value()) {
            SessionManager.getSession(option.clientId()).waitingConfirm(msg);
            EventManager.register(new EventKey(EventType.PUBLIST_COMPLETE, option.clientId()), msgAck ->
                    SessionManager.getSession(option.clientId()).confirmMessage(msg.variableHeader().messageId()));
        }
    }

    /**
     * cancel a subscription
     *
     * @param topic 主题
     */
    public void unsubscribe(String topic) {
        unsubscribe(new String[]{topic});
    }

    /**
     * cancel some subscription
     *
     * @param filters 主题表达式
     */
    public void unsubscribe(String[] filters) {
        checkConnect();
        MqttFixedHeader fixedHeader = FixHeaderUtil.from(MqttMessageType.UNSUBSCRIBE);
        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(IDUtil.nextAvailableId(option.clientId()));
        MqttUnsubscribePayload payload = new MqttUnsubscribePayload(Arrays.asList(filters));

        MqttUnsubscribeMessage msg = new MqttUnsubscribeMessage(fixedHeader, variableHeader, payload);

        sendMessage(msg);

        SessionManager.getSession(option.clientId()).waitingConfirm(msg);
        EventManager.register(new EventKey(EventType.UNSUBSCRIBE_SUCCESS, option.clientId()), (message) -> {
            MqttUnsubAckMessage ack = (MqttUnsubAckMessage) message;
            SessionManager.getSession(option.clientId()).confirmMessage(ack.variableHeader().messageId());
        });
    }

    /**
     * disconnect with server
     */
    public void disconnect() {
        checkConnect();
        sendMessage(new MqttMessage(FixHeaderUtil.from(MqttMessageType.DISCONNECT)));
        SessionManager.removeSession(option.clientId());
        clientCount.decrementAndGet();
    }

    /**
     * return the connection status
     *
     * @return isConnect
     */
    public boolean isConnect() {
        return isConnect;
    }

    /**
     * get clientId
     *
     * @return clientId
     */
    public String getClientId() {
        return option.clientId();
    }

    /**
     * when something than the listener intrest happens , listener will be call
     *
     * @param callback callback
     * @param type     type {@code EventType}
     */
    public void addCallback(EventType type, EventCallback callback) {
        EventManager.register(new EventKey(type, option.clientId()), callback);
    }

    /**
     * remove callback
     *
     * @param type {@code EventType}
     */
    public void removeCallback(EventType type) {
        EventManager.unregister(new EventKey(type, option.clientId()));
    }

    /**
     * 返回client的配置
     *
     * @return {@link MqttOption}
     */
    public MqttOption option() {
        return option;
    }

    public void close() {
        if (isConnect)
            disconnect();
        if (!option.shareBoot() || clientCount.get() == 1) {
            try {
                priBoot.config().group().shutdownGracefully().sync();
            } catch (InterruptedException e) {
            }
        }
    }

    private void doConnect() {
        MqttFixedHeader fixedHeader = FixHeaderUtil.from(MqttMessageType.CONNECT);
        MqttConnectVariableHeader variableHeader = new MqttConnectVariableHeader(
                MqttVersion.MQTT_3_1_1.protocolName(),
                MqttVersion.MQTT_3_1_1.protocolLevel(),
                option.hasUserInfo(),
                option.hasUserInfo(),
                option.willRetain(),
                option.willQos().value(),
                option.willFlag(),
                option.cleanSession(),
                option.keepAlive()
        );
        MqttConnectPayload payload = new MqttConnectPayload(
                option.clientId(),
                option.willTopic(),
                option.willMessage(),
                option.username(),
                option.password()
        );
        MqttConnectMessage msg = new MqttConnectMessage(fixedHeader, variableHeader, payload);

        sendMessage(msg);

        sync(new EventKey(EventType.CONNECT_SUCCESS, option.clientId()));

        isConnect = true;

        SessionManager.createSession(option.clientId());
    }

    private void initBoot() {

        if (option.shareBoot()) {
            lock.readLock().lock();
            if (shareBoot == null) {
                lock.readLock().unlock();
                lock.writeLock().lock();
                if (shareBoot == null) {
                    shareBoot = createBoot();
                }
                priBoot = shareBoot;
                lock.writeLock().unlock();
            } else {
                priBoot = shareBoot;
                lock.readLock().unlock();
            }
            clientCount.incrementAndGet();
        } else {
            priBoot = createBoot();
        }
    }

    private Bootstrap createBoot() {
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        Bootstrap boot = new Bootstrap();
        boot.group(workerGroup);
        boot.channel(NioSocketChannel.class);
        boot.option(ChannelOption.SO_KEEPALIVE, true);
        boot.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addFirst(new HeartBeatHandler());
                pipeline.addFirst(new IdleStateHandler(0, 0, option.keepAlive()));
                pipeline.addLast(new MqttDecoder());
                pipeline.addLast(MqttEncoder.INSTANCE);
                pipeline.addLast(new MqttClientHandler());

                // 如果配置了SSL相关信息，则加入SslHandler
                initSSL(pipeline);
            }
        });

        return boot;
    }

    private void initChannel() {
        try {
            channel = priBoot.connect(option.host(), option.port()).sync().channel();
            ChannelUtil.clientId(channel, option.clientId());
        } catch (InterruptedException e) {
            log.error("init channel failed", e);
        }
    }

    private void initSSL(ChannelPipeline pipeline) {

        if (!option.hasSslInfo())
            return;

        TrustManagerFactory tmf = null;
        KeyManagerFactory kmf = null;
        InputStream is = null;

        if (option.keyPath().startsWith("classpath:")) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            String keyPath = option.keyPath().replace("classpath:", "");
            is = cl.getResourceAsStream(keyPath);
        } else {
            try {
                is = new FileInputStream(new File(option.keyPath()));
            } catch (FileNotFoundException e) {
            }
        }

        if (is == null) {
            log.error("SSL key file not found at : {}", option.keyPath());
            return;
        }

        try {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(is, option.keyPass().toCharArray());
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);

            KeyManager[] keyManagers = null;
            if (!option.clientMode()) {
                kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(ks, option.keyPass().toCharArray());
                keyManagers = kmf.getKeyManagers();
            }

            SSLContext ssl = SSLContext.getInstance(MqttOption.SSL);
            ssl.init(keyManagers, tmf.getTrustManagers(), null);
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

    private void sendMessage(MqttMessage msg) {
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
}
