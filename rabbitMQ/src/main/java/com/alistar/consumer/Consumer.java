package com.alistar.consumer;

import com.alistar.unitl.ConnectionUtil;
import com.rabbitmq.client.*;

import java.io.IOException;


public class Consumer {

	public void getMessage() throws Exception {
		Connection connetction = ConnectionUtil.getConnetction();
		Channel channel = connetction.createChannel();
		channel.queueDeclare(ConnectionUtil.QUEUE_NAME, false, false, false, null);
		DefaultConsumer defaultConsumer = new DefaultConsumer(channel){
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
				System.out.println("Consumer 消费消息 " + new String(body, "UTF-8"));
			}
		};
		//消费消息
		channel.basicConsume(ConnectionUtil.QUEUE_NAME, defaultConsumer);
	}

}
