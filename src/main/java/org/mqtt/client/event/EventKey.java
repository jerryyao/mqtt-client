package org.mqtt.client.event;

import org.mqtt.client.util.Config;

import io.netty.channel.Channel;

public class EventKey {

	private EventType type;
	private Channel channel;

	public EventKey(EventType type, Channel channel) {
		this.type = type;
		this.channel = channel;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof EventKey)) {
			return false;
		}

		EventKey other = (EventKey) obj;

		if (this.type == other.type && this.channel == other.channel) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hascode = 17;
		hascode = 31 * hascode + type.hashCode();
		hascode = 31 * hascode + channel.hashCode();
		return hascode;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append(type.toString());
		builder.append(":");
		builder.append(channel.attr(Config.CLIENT_ID).get());
		builder.append("}");
		return builder.toString();
	}
}
