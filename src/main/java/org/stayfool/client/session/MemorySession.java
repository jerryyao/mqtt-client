package org.stayfool.client.session;

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import org.stayfool.client.MqttOption;
import org.stayfool.client.event.EventKey;
import org.stayfool.client.event.EventManager;
import org.stayfool.client.event.EventType;
import org.stayfool.client.util.TimeOutMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by stayfool on 2016/12/5.
 */
public class MemorySession implements Session<Object, Object> {

    private MqttOption option;
    private TimeOutMap<Integer, MqttMessage> waitingForConfirmMap = new TimeOutMap<>();
    private ConcurrentMap<Integer, MqttPublishMessage> retainedMap = new ConcurrentHashMap<>();
    private ConcurrentMap<Object, Object> attrMap = new ConcurrentHashMap<>();

    @Override
    public String clientId() {
        return option.clientId();
    }

    @Override
    public void retainMessage(MqttPublishMessage msg) {
        retainedMap.put(msg.variableHeader().messageId(), msg);
    }

    @Override
    public MqttPublishMessage removeRetain(int id) {
        return retainedMap.remove(id);
    }

    @Override
    public List<MqttPublishMessage> retainedMessage() {
        return new ArrayList<>(retainedMap.values());
    }

    @Override
    public void waitingConfirm(MqttMessage msg) {
        waitingForConfirmMap.put(getId(msg), msg);
    }

    @Override
    public MqttMessage confirmMessage(int id) {
        return waitingForConfirmMap.remove(id);
    }

    @Override
    public List<MqttMessage> waitingConfirmMessage() {
        return new ArrayList<>(waitingForConfirmMap.values());
    }

    @Override
    public void set(Object key, Object value) {
        attrMap.putIfAbsent(key, value);
    }

    @Override
    public Object get(Object key) {
        return attrMap.get(key);
    }

    private int getId(MqttMessage msg) {
        Object vheader = msg.variableHeader();
        if (vheader instanceof MqttPublishVariableHeader)
            return ((MqttPublishVariableHeader) msg.variableHeader()).messageId();
        else
            return ((MqttMessageIdVariableHeader) msg.variableHeader()).messageId();
    }

    @Override
    public int messageTimeout() {
        return option.messageTimeout();
    }

    @Override
    public void init(MqttOption option) {
        Objects.requireNonNull(option);
        waitingForConfirmMap = new TimeOutMap<>(option.messageTimeout(), (k, v, t) -> {
            EventManager.notify(new EventKey(EventType.MESSAGE_TIMEOUT, option.clientId()), v);
            confirmMessage(k);
        });
        this.option = option;
    }
}
