package com.lyj.bi.componet;

import jakarta.annotation.Resource;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

import static com.lyj.bi.MQconstant.MQContant.DELAY_EXCHANGE_NAME;
import static com.lyj.bi.MQconstant.MQContant.DELAY_QUEUE_ROUTING_A_NAME;


/**
 * 延迟队列生产者，消息延迟模拟
 */
@Service
public class DelayedMessageProducer {

    @Resource
    private AmqpTemplate amqpTemplate;

    public void sendDelayedMessage(String message, String delayTime) {
        amqpTemplate.convertAndSend(DELAY_EXCHANGE_NAME, DELAY_QUEUE_ROUTING_A_NAME, message, messagePostProcessor -> {
            //设置消息的过期时间，单位是ms
            messagePostProcessor.getMessageProperties().setExpiration(delayTime);
            return messagePostProcessor;
        });
    }
}
