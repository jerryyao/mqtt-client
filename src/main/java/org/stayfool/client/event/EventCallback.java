package org.stayfool.client.event;

/**
 * Created by stayfool on 2016/12/6.
 */
public interface EventCallback<T> {
    void callback(T msg);
}
