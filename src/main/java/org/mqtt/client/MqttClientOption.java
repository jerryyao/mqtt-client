package org.mqtt.client;

import io.netty.util.AttributeKey;
import io.netty.util.internal.StringUtil;
import org.mqtt.client.message.AbstractMessage;
import org.mqtt.client.message.QOSType;

/**
 * Created by stayfool on 2016/11/30.
 *
 * @author stayfool
 *         <p>
 *         MQTT client configuration
 */
public class MqttClientOption {

    public static final String SSL = "SSL";
    public static final String TLS = "TLS";
    public static final String JKS = "JKS";
    public static final AttributeKey<String> CLIENT_ID = AttributeKey.newInstance("CLIENT_ID");

    // client common info
    private String clientId = String.valueOf(System.currentTimeMillis());
    private boolean shareBoot = true;
    private String host;
    private int port;
    private String username;
    private String password;
    private int keepAlive = 30;
    private boolean cleanSession = true;
    private boolean willFlag = false;
    private QOSType willQos;
    private boolean willRetain = false;
    private String willTopic;
    private String willMessage;

    // SSL info
    private String keyPath;
    private String keyPass;
    private boolean clientMode = true;


    public MqttClientOption clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public MqttClientOption shareBoot(boolean shareBoot) {
        this.shareBoot = shareBoot;
        return this;
    }

    public MqttClientOption host(String host) {
        this.host = host;
        return this;
    }

    public MqttClientOption port(int port) {
        this.port = port;
        return this;
    }

    public MqttClientOption username(String username) {
        this.username = username;
        return this;
    }

    public MqttClientOption password(String password) {
        this.password = password;
        return this;
    }

    public MqttClientOption keepAlive(int keepAlive) {
        this.keepAlive = keepAlive;
        return this;
    }

    public MqttClientOption cleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
        return this;
    }

    public MqttClientOption willFlag(boolean willFlag) {
        this.willFlag = willFlag;
        return this;
    }

    public MqttClientOption willQos(QOSType willQos) {
        this.willQos = willQos;
        return this;
    }

    public MqttClientOption willRetain(boolean willRetain) {
        this.willRetain = willRetain;
        return this;
    }

    public MqttClientOption willTopic(String willTopic) {
        this.willTopic = willTopic;
        return this;
    }

    public MqttClientOption willMessage(String willMessage) {
        this.willMessage = willMessage;
        return this;
    }

    public MqttClientOption keyPath(String keyPath) {
        this.keyPath = keyPath;
        return this;
    }

    public MqttClientOption keyPass(String keyPass) {
        this.keyPass = keyPass;
        return this;
    }

    public MqttClientOption clientMode(boolean clientMode) {
        this.clientMode = clientMode;
        return this;
    }

    public String clientId() {
        return clientId;
    }

    public boolean shareBoot() {
        return shareBoot;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public int keepAlive() {
        return keepAlive;
    }

    public boolean cleanSession() {
        return cleanSession;
    }

    public boolean willFlag() {
        return willFlag;
    }

    public QOSType willQos() {
        return willQos;
    }

    public boolean willRetain() {
        return willRetain;
    }

    public String willTopic() {
        return willTopic;
    }

    public String willMessage() {
        return willMessage;
    }

    public String keyPath() {
        return keyPath;
    }

    public String keyPass() {
        return keyPass;
    }

    public boolean clientMode() {
        return clientMode;
    }

    public boolean validate() {
        return !(port <= 0 || StringUtil.isNullOrEmpty(host));
    }

    public boolean useSSL() {
        return !(StringUtil.isNullOrEmpty(keyPath) || StringUtil.isNullOrEmpty(keyPass));
    }

    private MqttClientOption() {
    }

    public static MqttClientOption instance() {
        return new MqttClientOption();
    }
}
