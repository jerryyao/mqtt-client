package org.stayfool.client.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Created by stayfool on 2016/12/8.
 * <p>
 * 一个具有超时功能的MAP
 */
public class TimeOutMap<K, V> {

    private static final int DEFAULT_TIMEOUT = 0;
    private static final int DEFAULT_POOLSIZE = 4;
    private static final int DEFAULT_CAPACITY = 16;
    private final Future emptyFuture = new EmptyFuture();
    private final TimeoutCallback<K, V> emptyCallback = new EmptyCallback<>();

    private final int timeout;
    private final ConcurrentMap<K, InternalValue> internalValueMap;
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
        this(timeout, DEFAULT_POOLSIZE, null);
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

        checkNull(callback);

        this.timeout = timeout;
        this.callback = callback;
        internalValueMap = new ConcurrentHashMap<>(capacity);
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
    public void put(K key, V value) {
        put(key, value, timeout);
    }

    /**
     * 添加对象到MAP,如果{@code timeout} <= 0, 则不超时
     *
     * @param key     键
     * @param value   值
     * @param timeout 超时时间（秒）
     */
    public void put(K key, V value, int timeout) {

        put(key, value, timeout, callback);
    }

    /**
     * 添加对象到MAP,如果{@code timeout} <= 0, 则不超时
     *
     * @param key      键
     * @param value    值
     * @param timeout  超时时间（秒）
     * @param callback 超时回调
     */
    public void put(K key, V value, int timeout, final TimeoutCallback<K, V> callback) {
        checkNull(callback);

        Future future = emptyFuture;
        if (timeout > 0) {
            future = scheduledService.schedule(() -> {
                callback.callback(key, value, timeout);
                remove(key);
                log.debug("value hold timeout, key : {}, value : {}, timeout : {}", key, value, timeout);
            }, timeout, TimeUnit.SECONDS);
        }
        internalValueMap.put(key, new InternalValue(future, value));
    }

    /**
     * @param key 键
     */
    public V get(K key) {

        if (internalValueMap.containsKey(key)) {
            return internalValueMap.get(key).value;
        }
        return null;
    }

    /**
     * @param key 键
     */
    public void remove(K key) {
        if (internalValueMap.containsKey(key)) {
            internalValueMap.get(key).future.cancel(true);
            log.debug("remove key : {}", key);
        }
        internalValueMap.remove(key);
    }

    /**
     * Returns the number of key-value mappings in this map.  If the
     * map contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of key-value mappings in this map
     */
    public int size() {
        return internalValueMap.size();
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings
     */
    public boolean isEmpty() {
        return internalValueMap.isEmpty();
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified
     * key.  More formally, returns <tt>true</tt> if and only if
     * this map contains a mapping for a key <tt>k</tt> such that
     * <tt>(key==null ? k==null : key.equals(k))</tt>.  (There can be
     * at most one such mapping.)
     *
     * @param key key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified
     * key
     * @throws ClassCastException   if the key is of an inappropriate type for
     *                              this map
     *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified key is null and this map
     *                              does not permit null keys
     *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     */
    public boolean containsKey(Object key) {
        return internalValueMap.containsKey(key);
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <tt>remove</tt> operation), the results of
     * the iteration are undefined.  The set supports element removal,
     * which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
     * operations.  It does not support the <tt>add</tt> or <tt>addAll</tt>
     * operations.
     *
     * @return a set view of the keys contained in this map
     */
    public Set<K> keySet() {
        return internalValueMap.keySet();
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  If the map is
     * modified while an iteration over the collection is in progress
     * (except through the iterator's own <tt>remove</tt> operation),
     * the results of the iteration are undefined.  The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
     * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not
     * support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a collection view of the values contained in this map
     */
    public Collection<V> values() {
        List<V> result = new ArrayList<>(size());
        internalValueMap.values().forEach(i -> result.add(i.value));
        return result;
    }

    /**
     * Removes all of the mappings from this map (optional operation).
     * The map will be empty after this call returns.
     *
     * @throws UnsupportedOperationException if the <tt>clear</tt> operation
     *                                       is not supported by this map
     */
    public void clear() {
        internalValueMap.values().forEach(i -> i.future.cancel(true));
        internalValueMap.clear();
    }

    /**
     * 设置超时回调
     *
     * @param callback 回调
     */
    public void setCallback(TimeoutCallback<K, V> callback) {
        checkNull(callback);
        this.callback = callback;
    }

    /**
     * 移除超时回调
     */
    public void removeCallback() {
        this.callback = null;
    }

    private void checkNull(Object obj) {
        if (obj == null)
            throw new IllegalArgumentException("callback can not be null");
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
    private final class EmptyFuture implements Future {

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
        public V get() throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }
    }

    public interface TimeoutCallback<K, V> {
        void callback(K key, V value, int timeout);
    }

    // 空回调
    private final class EmptyCallback<K, V> implements TimeoutCallback<K, V> {
        @Override
        public void callback(K key, V value, int timeout) {

        }
    }
}
