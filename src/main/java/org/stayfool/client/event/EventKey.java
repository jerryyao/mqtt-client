package org.stayfool.client.event;

public class EventKey {

    private EventType type;
    private String clientId;

    public EventKey(EventType type, String clientId) {
        this.type = type;
        this.clientId = clientId;
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

        if (this.type == other.type && this.clientId.equals(other.clientId)) {
            return true;
        }
        return false;
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
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append(type.toString());
        builder.append(":");
        builder.append(clientId);
        builder.append("}");
        return builder.toString();
    }
}
