package org.mqtt.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.handler.timeout.IdleStateHandler;
import org.mqtt.client.handler.MqttClientHandler;
import org.mqtt.client.handler.TimeOutHandler;
import org.mqtt.client.parser.MQTTDecoder;
import org.mqtt.client.parser.MQTTEncoder;
import org.mqtt.client.util.Config;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class MqttClientFactory {

	private static volatile AtomicInteger statistics = new AtomicInteger(0);
	private static volatile Bootstrap boot;
	private static volatile List<Bootstrap> bootHold = new CopyOnWriteArrayList<>();

	public static Bootstrap createBoot(boolean shareable) {
		if (!shareable) {
			return initBoot();
		}
		if (boot == null || statistics.get() > Config.connection_num_per_thread) {
			boot = initBoot();
			bootHold.add(boot);
			statistics.set(0);
		}

		statistics.incrementAndGet();
		return boot;
	}

	private static Bootstrap initBoot() {
		NioEventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			boot = new Bootstrap();
			boot.group(workerGroup);
			boot.channel(NioSocketChannel.class);
			boot.option(ChannelOption.SO_KEEPALIVE, true);
			boot.handler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addFirst(new TimeOutHandler());
					pipeline.addFirst(new IdleStateHandler(0,0,Config.keep_alive));
					pipeline.addLast(new MQTTDecoder());
					pipeline.addLast(new MQTTEncoder());
					pipeline.addLast(new MqttClientHandler());
				}
			});
		} catch (Exception ex) {
			workerGroup.shutdownGracefully();
			throw new RuntimeException(ex);
		}
		return boot;
	}

	public static MqttClient createShareBootClient() {
		MqttClient client = new MqttClient();
		client.setBootShareable(true);
		return client;
	}
}
