package org.stayfool.client.event;

import org.stayfool.client.message.ConnAckMessage;
import org.stayfool.client.message.PublishMessage;

public interface EventListener {

    default void connectSuccess() {
    }

    default void connectFailure(ConnAckMessage msg) {
    }

    default void publishSuccess() {
    }

    default void publishFailure() {
    }

    default void subscribeSuccess(String topic) {
    }

    default void subscribeFailure(String topic) {
    }

    default void disconnect() {
    }

    default void messageArrive(PublishMessage msg) {
    }
}
