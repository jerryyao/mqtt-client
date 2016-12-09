package org.stayfool.mqtt.test;

import org.stayfool.client.util.TimeOutMap;

import java.util.Map;

/**
 * Created by stayfool on 2016/12/9.
 */
public class TimeOutMapTest {


    public void tetss(){
        TimeOutMap.TimeoutCallback df= new TimeOutMap.TimeoutCallback() {
            @Override
            public void callback(Object key, Object value, int timeout) {

            }
        };
    }
}
