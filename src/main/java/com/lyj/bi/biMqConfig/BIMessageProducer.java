package com.lyj.bi.biMqConfig;

import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.lyj.bi.MQconstant.BiMqContant.*;


/**
 * 消息发送生产者，
 */
@Component
public class BIMessageProducer {
    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息
     * 绑定正常交换机，并设置消息超时时间
     * @param message 消息内容
     */
    public void sendMessage(String message) {
        // 使用convertAndSend方法将消息发送到指定的交换机和路由键
        //rabbitTemplate.convertAndSend(BI_EXCHANGE, BI_ROUTING_KEY, message);
        rabbitTemplate.convertAndSend(BI_EXCHANGE, BI_ROUTING_KEY, message, messagePostProcessor -> {
            //设置消息的过期时间，单位是ms
            messagePostProcessor.getMessageProperties().setExpiration(String.valueOf(BI_EXPIRE_TTL));
            return messagePostProcessor;
        });
    }
}
