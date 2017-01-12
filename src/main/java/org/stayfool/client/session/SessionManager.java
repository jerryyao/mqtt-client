package org.stayfool.client.session;

import org.stayfool.client.MqttOption;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by stayfool on 2016/12/5.
 */
public final class SessionManager {

    private static MqttOption option;

    private SessionManager() {
    }

    public static void init(MqttOption option) {
        SessionManager.option = option;
    }

    private static final ConcurrentMap<String, Session> sessionMap = new ConcurrentHashMap<>();

    /**
     * 获取已存在的Session
     * <p>
     * 不存在则为当前 {@code clientId} 创建一个{@code Session}
     *
     * @param clientId clientId
     * @return Session
     */
    public static Session getSession(String clientId) {
        if (sessionMap.containsKey(clientId))
            return sessionMap.get(clientId);

        Session s = null;
        try {
            s = option.session().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            //do nothing
        }
        if (s != null) {
            s.init(option);
            sessionMap.putIfAbsent(clientId, s);
        }
        return s;
    }

    /**
     * 清楚Session
     *
     * @param clientId clientId
     */
    public static void removeSession(String clientId) {
        sessionMap.remove(clientId);
    }

}
