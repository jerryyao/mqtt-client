package org.mqtt.client.event;

import org.mqtt.client.message.AbstractMessage;

/**
 * Created by pactera on 2016/11/17.
 */
public interface EventCallback {
	void callback(AbstractMessage msg);
}
