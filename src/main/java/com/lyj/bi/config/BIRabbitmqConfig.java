package com.lyj.bi.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.lyj.bi.MQconstant.BiMqContant.*;


/**
 * bi项目消息队列初始化
 */
@Component
public class BIRabbitmqConfig {

    //创建延迟交换器
    @Bean("bidelayExchange")
    public DirectExchange bidelayExchange(){
        return new DirectExchange(BI_EXCHANGE);
    }

    //创建延迟队列
    @Bean("bidelayQueue")
    public Queue bidelayQueue(){
        Map<String,Object> args=new HashMap<>();
        //绑定死信交换机
        args.put("x-dead-letter-exchange",BI_DEAD_EXCHANGE);
        //绑定死信队列的路由键
        args.put("x-dead-letter-routing-key",DEAD_LETTER_QUEUE_ROUTING_KEY);
        //todo 设置队列的最大长度,如果需要修改，需要删除原有队列
        args.put("x-max-length", 0);
        //TODO 设置队列过期时间,可用设置消息的过期时间代替
        //args.put("x-message-ttl", 10000);
        return QueueBuilder.durable(BI_QUEUE).withArguments(args).build();
    }

    //创建死信队列
    @Bean("bideadLetterQueue")
    public Queue bideadLetterQueue() {
        return new Queue(BI_DEAD_QUEUE);
    }


    //创建死信交换器
    @Bean("bideadExchange")
    public DirectExchange bideadLetterExchange(){
        return new DirectExchange(BI_DEAD_EXCHANGE);
    }

    //延迟队列绑定交换器
    @Bean
    public Binding bidelayQueueBinding(@Qualifier("bidelayQueue") Queue queue,@Qualifier("bidelayExchange")DirectExchange delayExchange){
        return BindingBuilder.bind(queue).to(delayExchange).with(BI_ROUTING_KEY);
    }

    //死信队列A绑定交换器
    @Bean
    public Binding bideadLetterQueueBinding(@Qualifier("bideadLetterQueue") Queue queue,@Qualifier("bideadExchange")DirectExchange deadExchange){
        return BindingBuilder.bind(queue).to(deadExchange).with(DEAD_LETTER_QUEUE_ROUTING_KEY);
    }

}
