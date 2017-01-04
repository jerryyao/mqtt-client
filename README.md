# mqtt-client

## 说明
-    此客户端是对broker进行压力测试开发的，支持单机创建大量的链接而只用很少的资源，这得益于netty的线程架构
-    使用netty实现，支持TCP，SSL

## 使用
-   如果需要创建大量的客户端，只需指定不同的clientId即可

-   DEMO
```java
    MqttClientOption option = MqttClientOption.instance().host("localhost").port(1883).clientId("client1");
    //client 1
    MqttClient client1 = new MqttClient(option);
    client1.connect();
    // client 2
    option.client("client2")
    MqttClient client2 = new MqttClient(option);
    client2.connect();
    // client n
    ......
```


