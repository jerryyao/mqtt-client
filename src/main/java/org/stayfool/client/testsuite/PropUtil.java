package org.stayfool.client.testsuite;

import java.util.ResourceBundle;

/**
 * Created on 2016/12/19.
 *
 * @author stayfool
 */
public class PropUtil {

    private static ResourceBundle res = ResourceBundle.getBundle("config");

    public static int getUserId() {
        return Integer.valueOf(res.getString("userId"));
    }

    public static int getProductId() {
        return Integer.valueOf(res.getString("produdtId"));
    }

    public static String getDeviceType() {
        return res.getString("deviceType");
    }

    public static String getSnBase() {
        return res.getString("snBase");
    }

    public static String getAccessToken() {
        return res.getString("accessToken");
    }

    public static String getTokenPassword() {
        return res.getString("accessTokenPassword");
    }

    public static int getClientNum() {
        return Integer.valueOf(res.getString("clientNum"));
    }

    public static String getBrokerAddress() {
        return res.getString("brokerAddress");
    }

    public static int getBrokerPort() {
        return Integer.valueOf(res.getString("brokerPort"));
    }

    public static String getKafkaAddress() {
        return res.getString("kafkaAddress");
    }

    public static int getKafkaPort() {
        return Integer.valueOf(res.getString("kafkaPort"));
    }

    public static String getHttpDeviceId() {
        return res.getString("httpDeviceId");
    }

    public static String getHttpUploadToken() {
        return res.getString("httpUploadToken");
    }

    public static String getHttpSn() {
        return res.getString("httpsn");
    }

    public static int getBrokerHttpport() {
        return Integer.valueOf(res.getString("brokerHttpport"));
    }
}
