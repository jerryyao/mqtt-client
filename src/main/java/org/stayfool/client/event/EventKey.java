package org.stayfool.client.event;

public class EventKey {

    private EventType type;
    private String clientId;

    public EventKey(EventType type, String clientId) {
        this.type = type;
        this.clientId = clientId;
    }

    public EventType type() {
        return type;
    }

    public String clientId() {
        return clientId;
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

        return this.type.equals(other.type) && this.clientId.equals(other.clientId);
    }

    @Override
    public int hashCode() {
        int hascode = 17;
        hascode = 31 * hascode + type.hashCode();
        hascode = 31 * hascode + clientId.hashCode();
        return hascode;
    }

    @Override
    public String toString() {
        return "{" + type.toString() + ":" + clientId + "}";
    }
}
