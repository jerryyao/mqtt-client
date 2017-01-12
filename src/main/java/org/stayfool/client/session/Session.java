package org.stayfool.client.session;

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;

import java.util.List;

/**
 * Created by stayfool on 2016/12/6.
 */
public interface Session<K, V> {

    /**
     * 获取客户端ID
     *
     * @return clientId
     */
    String clientId();

    /**
     * 保存消息
     *
     * @param msg 消息
     */
    void retainMessage(MqttPublishMessage msg);

    /**
     * 移除保留的消息
     *
     * @param id 消息ID
     * @return {@link MqttPublishMessage}
     */
    MqttPublishMessage removeRetain(int id);

    /**
     * 返回已保存的所有消息
     *
     * @return {@link List<MqttPublishMessage>}
     */
    List<MqttPublishMessage> retainedMessage();

    /**
     * 保存待确认的消息，如QOS1，QOS2未收到确认的消息
     *
     * @param msg 消息
     */
    void waitingConfirm(MqttMessage msg);

    /**
     * 收到消息后
     *
     * @param id 消息ID
     * @return 已确认的消息 {@link MqttMessage}
     */
    MqttMessage confirmMessage(int id);

    /**
     * 返回所有待确认的消息
     *
     * @return {@link List<MqttMessage>}
     */
    List<MqttMessage> waitingConfirmMessage();

    /**
     * 保存需要存储到session的数据
     *
     * @param key   键
     * @param value 值
     */
    void set(K key, V value);

    /**
     * 获取存储在session中的值
     *
     * @param key 键
     * @return key对应的值
     */
    <V> V get(K key);
}
