package org.stayfool.client.util;

import java.io.InputStream;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import io.netty.util.AttributeKey;

/**
 * read yml configuration into class
 * 
 * @author pactera
 *
 */
public class Config {

	public static final AttributeKey<String> CLIENT_ID = AttributeKey.newInstance("CLIENT_ID");

	public static final int connection_num;
	public static final int message_num_per_connection;
	public static final int connection_num_per_thread;
	public static final int keep_alive;
	public static final String username;
	public static final String password;
	public static final String server_url;
	public static final int server_port;
	public static final String server_protocol;
	public static final String publish_topic;
	public static final String subscribe_topic;
	public static final byte qos;

	static {
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.yml");
		Constructor constructor = new Constructor();
		PropertyUtils util = new PropertyUtils();
		util.setSkipMissingProperties(true);
		constructor.setPropertyUtils(util);
		Yaml yaml = new Yaml(constructor);
		ConfigBuider buider = yaml.loadAs(in, ConfigBuider.class);

		connection_num = buider.connection_num;
		message_num_per_connection = buider.message_num_per_connection;
		connection_num_per_thread = buider.connection_num_per_thread;
		keep_alive = buider.keep_alive;
		username = buider.username;
		password = buider.password;
		server_url = buider.server_url;
		server_port = buider.server_port;
		server_protocol = buider.server_protocol;
		publish_topic = buider.publish_topic;
		subscribe_topic = buider.subscribe_topic;
		qos = buider.qos;
	}

	private static class ConfigBuider {
		public int connection_num;
		public int message_num_per_connection;
		public int connection_num_per_thread;
		public int keep_alive;
		public String username;
		public String password;
		public String server_url;
		public int server_port;
		public String server_protocol;
		public String publish_topic;
		public String subscribe_topic;
		public byte qos;
	}
}
