package org.stayfool.client.event;

import org.stayfool.client.message.AbstractMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stayfool.client.message.ConnAckMessage;
import org.stayfool.client.message.PublishMessage;

import java.lang.reflect.Method;
import java.util.*;
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

    public static void unregister(EventKey key) {
        if (listenerMap.containsKey(key)) {
            listenerMap.remove(key);
            log.debug("event {} unregisted.", key);
        } else {
//			log.debug("no listener registed with key : {}", key);
        }
    }

    public static void notify(EventKey key, Object msg) {
        log.debug("notify event {}", key);
        if (!listenerMap.containsKey(key))
            return;

        List<EventCallback> callbackList = listenerMap.get(key);

        callbackList.stream().forEach(call -> executor.execute(() -> call.callback(msg)));
    }

    public static void registerListener(String clientId, EventListener listener) {

        register(new EventKey(EventType.MESSAGE_ARRIVE, clientId), (msg) -> listener.messageArrive((PublishMessage) msg));
        register(new EventKey(EventType.PUBLISH_SUCCESS, clientId), (msg) -> listener.publishSuccess());
        register(new EventKey(EventType.PUBLISH_FAILURE, clientId), (msg) -> listener.publishFailure());
        register(new EventKey(EventType.CONNECT_SUCCESS, clientId), (msg) -> listener.connectSuccess());
        register(new EventKey(EventType.CONNECT_FAILURE, clientId), (msg) -> listener.connectFailure((ConnAckMessage) msg));
        register(new EventKey(EventType.SUBSCRIBE_SUCCESS, clientId), (msg) -> listener.subscribeSuccess((String) msg));
        register(new EventKey(EventType.SUBSCRIBE_FAILURE, clientId), (msg) -> listener.subscribeFailure((String) msg));
        register(new EventKey(EventType.DIS_CONNECT, clientId), (msg) -> listener.disconnect());
    }

    public static void unregisterListener(String clientId) {

        Iterator<Map.Entry<EventKey, List<EventCallback>>> it = listenerMap.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<EventKey, List<EventCallback>> o = it.next();
            if (o.getKey().clientId().equals(clientId))
                it.remove();
        }

    }
}
