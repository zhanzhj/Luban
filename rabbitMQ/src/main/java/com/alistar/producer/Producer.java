package com.alistar.producer;

import com.alistar.unitl.ConnectionUtil;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

public class Producer {

	public void sendByExchange(String message) throws Exception {
		Connection connection = ConnectionUtil.getConnetction();
		Channel channel = connection.createChannel();
		//申明队列
		channel.queueDeclare(ConnectionUtil.QUEUE_NAME, false, false, false, null);
		//申明EXCHANGE
		channel.exchangeDeclare(ConnectionUtil.EXCHANGE_NAME, "fanout");
		//交换机和队列绑定
		channel.queueBind(ConnectionUtil.QUEUE_NAME, ConnectionUtil.EXCHANGE_NAME, "");

		channel.basicPublish(ConnectionUtil.EXCHANGE_NAME, "", null, message.getBytes());

		System.out.println("发送的消息为：" + message);
		channel.close();
		connection.close();
	}
}
