package org.stayfool.client.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * 一个简单的事件通知组件
 * <p>
 * Created by pactera on 2016/11/17.
 */
public class EventManager {

    private static ConcurrentMap<EventKey, List<EventCallback>> callbackMap = new ConcurrentHashMap<>();
    private static ExecutorService executor = Executors.newCachedThreadPool();
    private static Logger log = LoggerFactory.getLogger(EventManager.class);

    @SuppressWarnings("unchecked")
    public static void register(EventKey key, EventCallback callback) {
        if (callbackMap.containsKey(key))
            callbackMap.get(key).add(callback);
        else
            callbackMap.put(key, new CopyOnWriteArrayList<>(Collections.singletonList(callback)));

        log.debug("event {} registed.", key);
    }

    public static void unregister(EventKey key) {
        callbackMap.remove(key);
        log.debug("event {} unregisted.", key);
    }

    @SuppressWarnings("unchecked")
    public static void notify(EventKey key, Object msg) {
        log.debug("notify event {}", key);
        if (!callbackMap.containsKey(key)) {
            return;
        }

        List<EventCallback> callbackList = callbackMap.get(key);
        callbackList.forEach(call -> executor.execute(() -> call.callback(msg)));
    }

}
