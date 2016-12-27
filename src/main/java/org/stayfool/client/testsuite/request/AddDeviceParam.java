package org.stayfool.client.testsuite.request;

/**
 * Created by stayfool on 2016/12/16.
 */
public class AddDeviceParam {
    private Integer userId;
    private Integer productId;
    private String sn;
    private String deviceType;
    private String accessToken;
    private String accessPassword;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessPassword() {
        return accessPassword;
    }

    public void setAccessPassword(String accessPassword) {
        this.accessPassword = accessPassword;
    }
}
