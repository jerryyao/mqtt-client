# mqtt-client

## 说明
-    此客户端是对broker进行压力测试开发的，支持单机创建大量的链接而只用很少的资源，这得益于netty的线程架构
-    使用netty实现，支持TCP，SSL
-    暂未实现session
-    序列化[org.starfool.message],[org.stayfool.parser]使用[https://github.com/andsel/moquette](https://github.com/andsel/moquette)的组件，只做了简单的重构

## 使用
-   如果需要创建大量的客户端，只需指定不同的clientId即可

-   DEMO
    >       MqttClientOption option = MqttClientOption.instance().host("localhost").port(1883).clientId("client");
    >       MqttClient client = new MqttClient(option);
    >       client.connect();
    >       ......


