package org.stayfool.client;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stayfool.client.event.EventCallback;
import org.stayfool.client.event.EventKey;
import org.stayfool.client.event.EventManager;
import org.stayfool.client.event.EventType;
import org.stayfool.client.handler.HeartBeatHandler;
import org.stayfool.client.handler.MqttClientHandler;
import org.stayfool.client.message.*;
import org.stayfool.client.parser.MQTTDecoder;
import org.stayfool.client.parser.MQTTEncoder;
import org.stayfool.client.session.Session;
import org.stayfool.client.session.SessionManager;
import org.stayfool.client.util.ChannelUtil;
import org.stayfool.client.util.IDUtil;

import javax.net.ssl.*;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by pactera on 2016/11/30.
 *
 * @author stayfool
 */
public class MqttClient {

    private Logger log = LoggerFactory.getLogger(getClass());

    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static volatile Bootstrap boot;
    private Bootstrap priBoot;
    private Channel channel;
    private boolean isConnect;
    private MqttOption option;

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
    public void subscribe(String topic, QOSType qos) {
        subscribe(new String[]{topic}, qos);
    }

    /**
     * subscribe topics
     *
     * @param filters 主题表达式
     * @param qos     qos
     */
    public void subscribe(String[] filters, QOSType qos) {
        checkConnect();

        SubscribeMessage msg = new SubscribeMessage();
        msg.setMessageID(IDUtil.nextAvailableId(option.clientId()));
        for (String topic : filters)
            msg.addSubscription(new SubscribeMessage.Couple(qos.byteValue(), topic));

        sendMessage(msg);

    }

    /**
     * publish a message to a topic
     *
     * @param topic   主题
     * @param qos     qos
     * @param retain  是否保存
     * @param contect 消息内容
     */
    public void publish(String topic, QOSType qos, boolean retain, String contect) {
        checkConnect();

        PublishMessage msg = new PublishMessage();
        msg.setMessageID(IDUtil.nextAvailableId(option.clientId()));
        msg.setQos(qos);
        msg.setTopicName(topic);
        msg.setPayload(ByteBuffer.wrap(contect.getBytes(CharsetUtil.UTF_8)));
        msg.setRetainFlag(retain);

        sendMessage(msg);

        if (qos.byteValue() > QOSType.MOST_ONE.byteValue()) {
            SessionManager.getSession(option.clientId()).waitingForCommit(msg.getMessageID(), msg);
            EventManager.register(new EventKey(EventType.PUBLIST_COMPLETE, option.clientId()), msgAck ->
                    SessionManager.getSession(option.clientId()).commit(msg.getMessageID()));
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
        SessionManager.removeSession(option.clientId());
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
     * @param key      key
     */
    public void addCallback(EventKey key, EventCallback callback) {
        EventManager.register(key, callback);
    }

    /**
     * remove callback
     *
     * @param key {@code EventKy}
     */
    public void removeCallback(EventKey key) {
        EventManager.unregister(key);
    }

    private void doConnect() {
        ConnectMessage msg = new ConnectMessage();
        msg.setProtocolVersion((byte) 4);
        msg.setClientID(option.clientId());
        msg.setCleanSession(option.cleanSession());
        msg.setKeepAlive(option.keepAlive()); // secs
        msg.setWillFlag(option.willFlag());

        if (option.hasUserInfo()) {
            msg.setUserFlag(true);
            msg.setPasswordFlag(true);
            msg.setUsername(option.username());
            msg.setPassword(option.password().getBytes());
        }

        if (option.willFlag()) {
            msg.setWillQos(option.willQos().byteValue());
            msg.setWillRetain(option.willRetain());
            msg.setWillTopic(option.willTopic());
            msg.setWillMessage(option.willMessage().getBytes(CharsetUtil.UTF_8));
        }

        sendMessage(msg);

        sync(new EventKey(EventType.CONNECT_SUCCESS, option.clientId()));

        isConnect = true;

        Session session = SessionManager.createSession(option.clientId());
        session.clientId(option.clientId());
        session.channel(channel);
        session.connect();
    }

    private void initBoot() {

        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            lock.readLock().lock();
            if (boot != null && option.shareBoot()) {
                priBoot = boot;
                lock.readLock().unlock();
                return;
            }

            lock.readLock().unlock();
            lock.writeLock().lock();

            if (boot != null && option.shareBoot()) {
                priBoot = boot;
                lock.writeLock().unlock();
                return;
            }

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

            priBoot = boot;
            lock.writeLock().unlock();
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
            channel = priBoot.connect(option.host(), option.port()).sync().channel();
            ChannelUtil.clientId(channel, option.clientId());

            Thread.sleep(5000);
        } catch (InterruptedException e) {
            log.error("init channel failed", e);
            exit();
        }
    }

    private void initSSL(ChannelPipeline pipeline) {

        if (!option.hasSslInfo())
            return;

        TrustManagerFactory tmf = null;
        KeyManagerFactory kmf = null;

        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(option.keyPath())) {

            KeyStore ks = KeyStore.getInstance(MqttOption.JKS);
            ks.load(in, option.keyPass().toCharArray());
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

    private void syncOr(List<EventKey> eventKeys) {

        final CountDownLatch connectLatch = new CountDownLatch(1);

        eventKeys.forEach(key -> EventManager.register(key, (msg) -> connectLatch.countDown()));
        try {
            connectLatch.await();
        } catch (InterruptedException e) {
            log.error("sync {} failed, cause : {} ", eventKeys, e);
        }
    }

    private void exit() {
        System.exit(-1);
    }
}
