package org.stayfool.client.testsuite;

import com.alibaba.fastjson.JSON;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stayfool.client.testsuite.request.AddDeviceParam;
import org.stayfool.client.testsuite.request.UploadDataParam;
import org.stayfool.client.testsuite.response.BaseResponse;
import org.stayfool.client.testsuite.response.IsOnlineRsp;

import java.io.IOException;
import java.util.UUID;

/**
 * Created on 2016/12/19.
 *
 * @author stayfool
 */
public class RestApiTestSuite {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private String checkOnline = "http://{host}:{port}/devices/status?deviceId={deviceId}";
    private String sendCmd = "http://{host}:{port}/devices/{deviceId}/cmd/json?commandId={commandId}";
    private String addDevice = "http://{host}:{port}/devices/";
    private String uploadData = "http://{host}:{port}/devicedata/json";

    public boolean checkOnlint(String deviceId) {
        String url = checkOnline
                .replace("{host}", PropUtil.getBrokerAddress())
                .replace("{port}", PropUtil.getBrokerHttpport() + "")
                .replace("{deviceId}", deviceId);

        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet(url);

        try {
            CloseableHttpResponse res = client.execute(get);

            BaseResponse obj = JSON.parseObject(EntityUtils.toString(res.getEntity()), BaseResponse.class);
            IsOnlineRsp on = JSON.parseObject(JSON.toJSONString(obj.getData()), IsOnlineRsp.class);
            if (on.isOnline()) {
                log.info("device : {} is online ", deviceId);
            } else
                log.info("device : {} is offline ");

            int code = res.getStatusLine().getStatusCode();
            return (code == 200 || code == 404);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean sendCmd(String deviceId) {
        String url = sendCmd
                .replace("{host}", PropUtil.getBrokerAddress())
                .replace("{port}", PropUtil.getBrokerHttpport() + "")
                .replace("{deviceId}", deviceId)
                .replace("{commandId}", UUID.randomUUID().toString().replaceAll("-", ""));

        WifiStatus cmd = new WifiStatus();
        cmd.setStrength(10);
        cmd.setConnectedNum(100);

        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(url);

        try {
            StringEntity s = new StringEntity(JSON.toJSONString(cmd));
            s.setContentEncoding("UTF-8");
            s.setContentType("application/json");
            post.setEntity(s);

            CloseableHttpResponse res = client.execute(post);

            if (res.getStatusLine().getStatusCode() == 200) {
                log.info("command {} send to {} success ", JSON.toJSONString(cmd), deviceId);
            } else {
                log.info("device {}  send to {} failed", JSON.toJSONString(cmd), deviceId);
            }
            int code = res.getStatusLine().getStatusCode();
            return (code == 200 || code == 404);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean addDevice() {
        String url = addDevice
                .replace("{host}", PropUtil.getBrokerAddress())
                .replace("{port}", PropUtil.getBrokerHttpport() + "");

        AddDeviceParam param = new AddDeviceParam();
        param.setUserId(PropUtil.getUserId());
        param.setProductId(PropUtil.getProductId());
        param.setSn(PropUtil.getSnBase() + "http");
        param.setDeviceType(PropUtil.getDeviceType());
        param.setAccessToken(PropUtil.getAccessToken());
        param.setAccessPassword(PropUtil.getTokenPassword());

        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(url);

        try {
            StringEntity s = new StringEntity(JSON.toJSONString(param));
            s.setContentEncoding("UTF-8");
            s.setContentType("application/json");
            post.setEntity(s);

            CloseableHttpResponse res = client.execute(post);

            if (res.getStatusLine().getStatusCode() == 200) {
                BaseResponse br = JSON.parseObject(EntityUtils.toString(res.getEntity()), BaseResponse.class);
                log.info("device add success, deviceId : ", br.getData());
            } else {
                log.info("device add failed");
            }
            int code = res.getStatusLine().getStatusCode();
            return (code == 200 || code == 404);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;

    }

    public boolean uploadData() {
        String url = uploadData
                .replace("{host}", PropUtil.getBrokerAddress())
                .replace("{port}", PropUtil.getBrokerHttpport() + "");

        WifiStatus cmd = new WifiStatus();
        cmd.setStrength(10);
        cmd.setConnectedNum(100);

        UploadDataParam param = new UploadDataParam();
        param.setData(JSON.toJSON(cmd));
        param.setDeviceId(PropUtil.getHttpDeviceId());
        param.setUploadToken(PropUtil.getHttpUploadToken());

        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(url);

        try {
            StringEntity s = new StringEntity(JSON.toJSONString(param));
            s.setContentEncoding("UTF-8");
            s.setContentType("application/json");
            post.setEntity(s);

            CloseableHttpResponse res = client.execute(post);

            if (res.getStatusLine().getStatusCode() == 200) {
                log.info("upload data success ");
            } else {
                log.info("uplosd data failed");
            }
            int code = res.getStatusLine().getStatusCode();
            return (code == 200 || code == 404);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;

    }
}
