package org.stayfool.client.message;

/**
 * Created by pactera on 2016/11/30.
 *
 * @author stayfool
 */
public enum QOSType {
    MOST_ONE, LEAST_ONE, EXACTLY_ONCE, RESERVED, FAILURE;

    public static QOSType valueOf(byte qos) {
        switch (qos) {
            case 0x00:
                return MOST_ONE;
            case 0x01:
                return LEAST_ONE;
            case 0x02:
                return EXACTLY_ONCE;
            case (byte) 0x80:
                return FAILURE;
            default:
                throw new IllegalArgumentException("Invalid QOS Type. Expected either 0, 1, 2, or 0x80. Given: " + qos);
        }
    }

    public byte byteValue() {
        switch (this) {
            case MOST_ONE:
                return 0;
            case LEAST_ONE:
                return 1;
            case EXACTLY_ONCE:
                return 2;
            case FAILURE:
                return (byte) 0x80;
            default:
                throw new IllegalArgumentException("Cannot give byteValue of QOSType: " + this.name());
        }
    }

    public static String formatQoS(QOSType qos) {
        return String.format("%d - %s", qos.byteValue(), qos.name());
    }
}
