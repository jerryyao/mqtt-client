package org.stayfool.client.session;

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by stayfool on 2016/12/5.
 */
public class MemorySession implements Session {

    private String clientId;
    private ConcurrentMap<Integer, MqttMessage> waitingForConfirmMap = new ConcurrentHashMap<>();
    private ConcurrentMap<Integer, MqttPublishMessage> retainedMap = new ConcurrentHashMap<>();

    public MemorySession(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public String clientId() {
        return clientId;
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

    private int getId(MqttMessage msg) {
        Object vheader = msg.variableHeader();
        if (vheader instanceof MqttPublishVariableHeader)
            return ((MqttPublishVariableHeader) msg.variableHeader()).messageId();
        else
            return ((MqttMessageIdVariableHeader) msg.variableHeader()).messageId();
    }
}
