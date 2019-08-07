package com.alistar.unitl;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class ConnectionUtil {

	public static final String QUEUE_NAME = "myQueue";
	public static final String EXCHANGE_NAME = "myExchange";

	public static Connection getConnetction() throws Exception{
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("127.0.0.1");
		factory.setPort(5672);
		factory.setUsername("guest");
		factory.setPassword("guest");
		return factory.newConnection();
	}
}
