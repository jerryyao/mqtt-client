package org.stayfool.client.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Created by stayfool on 2016/12/8.
 * <p>
 * 一个具有超时功能的MAP
 */
public class TimeOutMap<K, V> implements ConcurrentMap<K, V> {

    private static final int DEFAULT_TIMEOUT = 0;
    private static final int DEFAULT_POOLSIZE = 4;
    private static final int DEFAULT_CAPACITY = 16;
    private static final Future emptyFuture = new EmptyFuture();
    private static final TimeoutCallback emptyCallback = new EmptyCallback();

    private final int timeout;
    private final ConcurrentMap<K, InternalValue> valueMap;
    private final ScheduledExecutorService scheduledService;
    private volatile TimeoutCallback<K, V> callback = emptyCallback;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public TimeOutMap() {
        this(DEFAULT_TIMEOUT);
    }

    /**
     * @param timeout 超时时间
     */
    public TimeOutMap(int timeout) {
        this(timeout, DEFAULT_POOLSIZE, emptyCallback);
    }

    /**
     * @param timeout  超时时间
     * @param callback 超时回调
     */
    public TimeOutMap(int timeout, TimeoutCallback<K, V> callback) {
        this(timeout, DEFAULT_POOLSIZE, callback);
    }

    /**
     * @param timeout  超时时间
     * @param poolSize 调度线程池数量
     * @param callback 超时回调
     */
    public TimeOutMap(int timeout, int poolSize, TimeoutCallback<K, V> callback) {
        this(timeout, poolSize, poolSize, callback);
    }

    /**
     * @param timeout      超时时间
     * @param corePoolSize 调度线程池最小数量
     * @param maxPoolSize  调度线程池最大数量
     * @param callback     超时回调
     */
    public TimeOutMap(int timeout, int corePoolSize, int maxPoolSize, TimeoutCallback<K, V> callback) {
        this(timeout, corePoolSize, maxPoolSize, DEFAULT_CAPACITY, callback);
    }

    /**
     * @param timeout      超时时间
     * @param corePoolSize 调度线程池最小数量
     * @param maxPoolSize  调度线程池最大数量
     * @param capacity     MAP容量
     * @param callback     超时回调
     */
    public TimeOutMap(int timeout, int corePoolSize, int maxPoolSize, int capacity, TimeoutCallback<K, V> callback) {

        Objects.requireNonNull(callback);

        this.timeout = timeout;
        this.callback = callback;
        valueMap = new ConcurrentHashMap<>(capacity);
        scheduledService = Executors.newScheduledThreadPool(corePoolSize);
        ((ThreadPoolExecutor) scheduledService).setMaximumPoolSize(maxPoolSize);
    }

    /**
     * 添加键值对到MAP
     * 使用初始化时设置，没有设置默认不超时
     *
     * @param key   键
     * @param value 值
     */
    public V put(K key, V value) {
        return put(key, value, timeout);
    }

    /**
     * 添加对象到MAP,如果{@code timeout} <= 0, 则不超时
     *
     * @param key     键
     * @param value   值
     * @param timeout 超时时间（秒）
     */
    public V put(K key, V value, int timeout) {

        return put(key, value, timeout, callback);
    }

    /**
     * 添加对象到MAP,如果{@code timeout} <= 0, 则不超时
     *
     * @param key      键
     * @param value    值
     * @param timeout  超时时间（秒）
     * @param callback 超时回调
     */
    public V put(K key, V value, int timeout, final TimeoutCallback<K, V> callback) {
        Objects.requireNonNull(callback);

        Future future = emptyFuture;
        if (timeout > 0) {
            future = scheduledService.schedule(() -> {
                callback.callback(key, value, timeout);
                remove(key);
                log.debug("value hold timeout, key : {}, value : {}, timeout : {}", key, value, timeout);
            }, timeout, TimeUnit.SECONDS);
        }
        valueMap.put(key, new InternalValue(future, value));
        return value;
    }

    /**
     * @param key 键
     */
    public V get(Object key) {

        if (valueMap.containsKey(key)) {
            return valueMap.get(key).value;
        }
        return null;
    }

    /**
     * @param key 键
     */
    public V remove(Object key) {
        if (valueMap.containsKey(key)) {
            valueMap.get(key).future.cancel(true);
            log.debug("remove key : {}", key);
            return valueMap.remove(key).value;
        }
        return null;
    }

    public int size() {
        return valueMap.size();
    }

    public boolean isEmpty() {
        return valueMap.isEmpty();
    }

    public boolean containsKey(Object key) {
        return valueMap.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return valueMap.values().parallelStream().filter(i -> i.value.equals(value)).count() > 0;
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        m.entrySet().parallelStream().forEach(e -> put(e.getKey(), e.getValue()));
    }

    public Set<K> keySet() {
        return valueMap.keySet();
    }

    public Collection<V> values() {
        return valueMap.values().parallelStream().map(v -> v.value).collect(Collectors.toList());
    }

    public Set<Entry<K, V>> entrySet() {
        return valueMap.entrySet().parallelStream()
                .map(e -> new AbstractMap.SimpleEntry(e.getKey(), e.getValue().value))
                .collect(Collectors.toSet());
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;

        if (o instanceof TimeOutMap) {
            TimeOutMap other = (TimeOutMap) o;
            return this.valueMap.equals(other.valueMap)
                    && this.timeout == other.timeout
                    && this.callback.equals(other.callback)
                    && this.scheduledService.equals(other.scheduledService);
        }
        return false;
    }

    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + valueMap.hashCode();
        hash = hash * 31 + timeout;
        hash = hash * 31 + callback.hashCode();
        hash = hash * 31 + scheduledService.hashCode();
        return hash;
    }

    /**
     * Removes all of the mappings from this map (optional operation).
     * The map will be empty after this call returns.
     *
     * @throws UnsupportedOperationException if the <tt>clear</tt> operation
     *                                       is not supported by this map
     */
    public void clear() {
        valueMap.values().parallelStream().forEach(i -> i.future.cancel(true));
        valueMap.clear();
    }

    /**
     * 设置超时回调
     *
     * @param callback 回调
     */
    public void setCallback(TimeoutCallback<K, V> callback) {
        Objects.requireNonNull(callback);
        this.callback = callback;
    }

    /**
     * 移除超时回调
     */
    public void removeCallback() {
        this.callback = emptyCallback;
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        valueMap.forEach((key, value) -> action.accept(key, value.value));
    }

    public boolean replace(K key, V oldValue, V newValue) {
        if (valueMap.containsKey(key) && valueMap.get(key).value.equals(oldValue)) {
            put(key, newValue);
            return true;
        }
        return false;
    }

    public V replace(K key, V value) {
        V result = valueMap.containsKey(key) ? valueMap.get(key).value : null;
        put(key, value);
        return result;
    }

    public V putIfAbsent(K key, V value) {
        if (valueMap.containsKey(key)) {
            return valueMap.get(key).value;
        }
        put(key, value);
        return null;
    }

    public boolean remove(Object key, Object value) {
        if (containsKey(key) && get(key).equals(value)) {
            remove(key);
            return true;
        }
        return false;
    }

    // MAP内部实际存储对象
    private final class InternalValue {
        private final Future future;
        private final V value;

        InternalValue(Future future, V value) {
            this.future = future;
            this.value = value;
        }
    }

    // 空任务
    private static final class EmptyFuture implements Future {

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return true;
        }

        @Override
        public boolean isCancelled() {
            return true;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Object get() throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }
    }

    // 空回调
    private static final class EmptyCallback<K, V> implements TimeoutCallback<K, V> {
        @Override
        public void callback(K key, V value, int timeout) {

        }
    }

    // 超时回调接口
    public interface TimeoutCallback<K, V> {
        void callback(K key, V value, int timeout);
    }

}
