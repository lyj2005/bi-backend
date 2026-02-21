package com.lyj.bi.biMqConfig;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import static com.lyj.bi.MQconstant.BiMqContant.*;


/**
 * 用于测试程序用到的交换机和队列（只在程序启动前执行一次）
 * 已使用BIMqConfig类替代创建交换机和队列
 */
public class biInitMain {
    public static void main(String[] args) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("192.168.88.130");
            factory.setPort(5672);
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            String EXCHANGE_NAME = BI_EXCHANGE;
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");

            // 声明一个队列，并且设置持久化消息
            String queueName = BI_QUEUE;
            channel.queueDeclare(queueName, true, false, false, null);
            //队列绑定交换机，routing_key用于指定消息应该发送到哪个队列。
            channel.queueBind(queueName, EXCHANGE_NAME, BI_ROUTING_KEY);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
