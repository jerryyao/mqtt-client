package org.stayfool.client.util;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author stayfool
 * @date 2016/12/6
 */
public final class IDUtil {

    private static ConcurrentMap<String, AtomicInteger> idMap = new ConcurrentHashMap<>();

    /**
     * 获取下一个有效的ID
     *
     * @param clientId clientId
     * @return nextAvailableId
     */
    public static Integer nextAvailableId(String clientId) {

        if (idMap.containsKey(clientId)) {
            AtomicInteger messageIdHolder = idMap.get(clientId);
            messageIdHolder.compareAndSet(Integer.MAX_VALUE, 0);
            return messageIdHolder.incrementAndGet();
        }

        AtomicInteger messageIdHolder = new AtomicInteger(0);
        idMap.put(clientId, messageIdHolder);

        return messageIdHolder.incrementAndGet();
    }

    public static String uuid() {
        return UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
    }

    private IDUtil() {
    }
}
