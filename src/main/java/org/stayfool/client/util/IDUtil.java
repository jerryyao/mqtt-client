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
            messageIdHolder.compareAndSet(Integer.MAX_VALUE, Integer.MIN_VALUE);
            return messageIdHolder.getAndIncrement();
        }

        AtomicInteger messageIdHolder = new AtomicInteger(Integer.MIN_VALUE);
        idMap.put(clientId, messageIdHolder);

        return messageIdHolder.get();
    }

    public static String uuid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    private IDUtil() {
    }
}
