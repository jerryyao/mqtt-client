package org.stayfool.client.session;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by stayfool on 2016/12/5.
 */
public final class SessionManager {
    private SessionManager() {
    }

    private static final ConcurrentMap<String, Session> sessionMap = new ConcurrentHashMap<>();

    /**
     * 获取已存在的Session，不存在返回NULL
     *
     * @param clientId clientId
     * @return Session
     */
    public static Session getSession(String clientId) {
        if (sessionMap.containsKey(clientId))
            return sessionMap.get(clientId);

        return null;
    }

    /**
     * 清楚Session
     *
     * @param clientId clientId
     */
    public static void removeSession(String clientId) {
        sessionMap.remove(clientId);
    }

    /**
     * 为当前 {@code clientId} 创建一个{@code MemorySession}
     *
     * @param clientId clientId
     * @return Seesion
     */
    public static Session createSession(String clientId) {
        MemorySession session = new MemorySession(clientId);
        sessionMap.putIfAbsent(clientId, session);
        return session;
    }
}
