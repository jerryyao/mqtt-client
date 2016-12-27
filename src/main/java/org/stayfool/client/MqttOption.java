package org.stayfool.client;

import io.netty.util.internal.StringUtil;
import org.stayfool.client.message.QOSType;

/**
 * Created by stayfool on 2016/11/30.
 *
 * @author stayfool
 *         <p>
 *         MQTT client configuration
 */
public class MqttOption {

    public static final String SSL = "SSL";
    public static final String TLS = "TLS";
    public static final String JKS = "JKS";

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

    public MqttOption clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public MqttOption shareBoot(boolean shareBoot) {
        this.shareBoot = shareBoot;
        return this;
    }

    public MqttOption host(String host) {
        this.host = host;
        return this;
    }

    public MqttOption port(int port) {
        this.port = port;
        return this;
    }

    public MqttOption username(String username) {
        this.username = username;
        return this;
    }

    public MqttOption password(String password) {
        this.password = password;
        return this;
    }

    public MqttOption keepAlive(int keepAlive) {
        this.keepAlive = keepAlive;
        return this;
    }

    public MqttOption cleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
        return this;
    }

    public MqttOption willFlag(boolean willFlag) {
        this.willFlag = willFlag;
        return this;
    }

    public MqttOption willQos(QOSType willQos) {
        this.willQos = willQos;
        return this;
    }

    public MqttOption willRetain(boolean willRetain) {
        this.willRetain = willRetain;
        return this;
    }

    public MqttOption willTopic(String willTopic) {
        this.willTopic = willTopic;
        return this;
    }

    public MqttOption willMessage(String willMessage) {
        this.willMessage = willMessage;
        return this;
    }

    public MqttOption keyPath(String keyPath) {
        this.keyPath = keyPath;
        return this;
    }

    public MqttOption keyPass(String keyPass) {
        this.keyPass = keyPass;
        return this;
    }

    public MqttOption clientMode(boolean clientMode) {
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

    public boolean hasSslInfo() {
        return !(StringUtil.isNullOrEmpty(keyPath) || StringUtil.isNullOrEmpty(keyPass));
    }

    public boolean hasUserInfo() {
        return !(StringUtil.isNullOrEmpty(username) || StringUtil.isNullOrEmpty(password));
    }

    private MqttOption() {
    }

    public static MqttOption instance() {
        return new MqttOption();
    }
}
