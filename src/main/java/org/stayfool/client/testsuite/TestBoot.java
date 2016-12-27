package org.stayfool.client.testsuite;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created on 2016/12/20.
 *
 * @author stayfool
 */
public class TestBoot {

    public static void main(String[] arg) {
        ExecutorService es = Executors.newCachedThreadPool();

        KafkaTestSuite kafka = new KafkaTestSuite(es);
        kafka.consume();

        BrokerTestSuite broker = new BrokerTestSuite(es);
        broker.starTest();

        RestApiTestSuite api = new RestApiTestSuite();
        boolean online = api.checkOnlint(broker.randomExistsDevice());
        boolean addDevice = api.addDevice();
        boolean uploadData = api.uploadData();
        boolean sendCmd = api.sendCmd(broker.randomExistsDevice());

        int num = 0;
        while (!kafka.isComplete() && num < 10) {

            num++;
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        es.shutdown();
        String cp = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        File f = new File(cp);
        f = f.getParentFile();
        f = new File(f.getAbsolutePath() + "/result.txt");
        try {
            FileOutputStream os = new FileOutputStream(f);
            os.write(("isOnlineApi result : " + online).getBytes());
            os.write("\r\n".getBytes());
            os.write(("addDeviceApi result : " + addDevice).getBytes());
            os.write("\r\n".getBytes());
            os.write(("uploadDataApi result : " + uploadData).getBytes());
            os.write("\r\n".getBytes());
            os.write(("sendCmdApi result : " + sendCmd).getBytes());
            os.write("\r\n".getBytes());
            os.write(("kafka result : " + kafka.isComplete()).getBytes());
            os.write("\r\n".getBytes());
            os.flush();
            os.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.exit(0);

    }

}
