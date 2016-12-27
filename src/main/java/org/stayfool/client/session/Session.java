package org.stayfool.client.session;

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;

import java.util.List;

/**
 * Created by stayfool on 2016/12/6.
 */
public interface Session {

    String clientId();

    void retainMessage(MqttPublishMessage msg);

    MqttPublishMessage removeRetain(int id);

    List<MqttPublishMessage> retainedMessage();

    void waitingConfirm(MqttMessage msg);

    MqttMessage confirmMessage(int id);

    List<MqttMessage> waitingConfirmMessage();
}
