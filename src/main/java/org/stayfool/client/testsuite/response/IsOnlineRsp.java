package org.stayfool.client.testsuite.response;

/**
 * Created by szl on 2016/12/8.
 */
public class IsOnlineRsp {
    private boolean online;

    public IsOnlineRsp() {
    }

    public IsOnlineRsp(boolean online) {
        this.online = online;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}
