package com.lyj.bi.componet;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;

import static com.lyj.bi.MQconstant.MQContant.DEAD_LETTER_QUEUE_A_NAME;


/**
 * 死信队列消费者模拟消息延迟
 */
@Component
@Slf4j
public class QueueConsumer {



    /**
     * 死信队列消息处理
     * @param message
     * @param channel
     * @throws IOException
     */
    //使用该注解指定程序要监听的队列，，并设置消息的确认机制为手动
    @RabbitListener(queues= DEAD_LETTER_QUEUE_A_NAME,ackMode="MANUAL",autoStartup = "false")
    //在mq中，每条消息都会被分配一个唯一投递标签，用于标识该消息在通道中的投递状态和顺序，使用该注解可以从消息头中获取该投递标签，并将其赋值给deliveryTag参数，
    public void receiveA(Message message, Channel channel,@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException{
        String msg = new String(message.getBody());
        log.info("当前时间：{},死信队列A收到消息：{}", new Date(), msg);
        //channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        //表示消费者确认了具有指定deliveryTag的消息，并且不进行批量确认,只确认指定的deliveryTag对应的消息。
        channel.basicAck(deliveryTag,false);
    }
}
