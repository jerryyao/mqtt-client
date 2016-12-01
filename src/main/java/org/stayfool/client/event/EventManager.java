package org.stayfool.client.event;

import org.stayfool.client.message.AbstractMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 一个简单的事件通知组件
 * <p>
 * Created by pactera on 2016/11/17.
 */
public class EventManager {
    private static ConcurrentMap<EventKey, List<EventCallback>> listenerMap = new ConcurrentHashMap<>();
    private static ExecutorService executor = Executors.newCachedThreadPool();
    private static Logger log = LoggerFactory.getLogger(EventManager.class);

    public static void register(EventKey key, EventCallback callback) {
        if (listenerMap.containsKey(key))
            listenerMap.get(key).add(callback);
        else
            listenerMap.put(key, new ArrayList<>(Arrays.asList(callback)));

        log.debug("event {} registed.", key);
    }

    public static void unRegister(EventKey key) {
        if (listenerMap.containsKey(key)) {
            listenerMap.remove(key);
            log.debug("event {} unregisted.", key);
        } else {
//			log.debug("no listener registed with key : {}", key);
        }
    }

    public static void notify(EventKey key, AbstractMessage msg) {
        log.debug("notify event {}", key);
        if (!listenerMap.containsKey(key))
            return;

        List<EventCallback> callbackList = listenerMap.get(key);
        for (EventCallback callback : callbackList) {
            executor.execute(() -> callback.callback(msg));
        }
    }
}
