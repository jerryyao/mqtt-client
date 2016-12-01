package org.stayfool.client.message;

/**
 * Created by pactera on 2016/11/30.
 *
 * @author stayfool
 */
public final class MessageType {

    public static final byte CONNECT = 1; // Client request to connect to Server
    public static final byte CONNACK = 2; // Connect Acknowledgment
    public static final byte PUBLISH = 3; // Publish message
    public static final byte PUBACK = 4; // Publish Acknowledgment
    public static final byte PUBREC = 5; //Publish Received (assured delivery part 1)
    public static final byte PUBREL = 6; // Publish Release (assured delivery part 2)
    public static final byte PUBCOMP = 7; //Publish Complete (assured delivery part 3)
    public static final byte SUBSCRIBE = 8; //Client Subscribe request
    public static final byte SUBACK = 9; // Subscribe Acknowledgment
    public static final byte UNSUBSCRIBE = 10; //Client Unsubscribe request
    public static final byte UNSUBACK = 11; // Unsubscribe Acknowledgment
    public static final byte PINGREQ = 12; //PING Request
    public static final byte PINGRESP = 13; //PING Response
    public static final byte DISCONNECT = 14; //Client is Disconnecting

    private MessageType() {
        //单列
    }
}
