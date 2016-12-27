package org.stayfool.client.testsuite.request;

/**
 * Created by stayfool on 2016/12/16.
 */
public class UploadDataParam {
    private String deviceId;
    private String uploadToken;
    private Object data;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getUploadToken() {
        return uploadToken;
    }

    public void setUploadToken(String uploadToken) {
        this.uploadToken = uploadToken;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
