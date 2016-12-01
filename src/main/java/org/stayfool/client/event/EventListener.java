package org.stayfool.client.event;

import org.stayfool.client.message.PublishMessage;

public interface EventListener {

	default void connectSuccess() {
	}

	default void publishSuccess() {
	}

	default void subscribeSuccess() {
	}

    default void disconnect(){
    }

	default void messageArrive(PublishMessage msg) {
	}
}
