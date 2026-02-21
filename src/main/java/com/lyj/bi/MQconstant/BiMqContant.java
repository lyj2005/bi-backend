package com.lyj.bi.MQconstant;

public interface BiMqContant {
    //正常交换机
    String BI_EXCHANGE = "bi_delay_exchange";
    //正常队列
    String BI_QUEUE = "bi_delay_queue";
    //死信队列
    String BI_DEAD_QUEUE = "bi_dead_queue";
    //设置过期时间和任务处理超时时间
    long BI_EXPIRE_TTL = 10000;
    //死信交换机
    String BI_DEAD_EXCHANGE = "bi_dead_exchange";
    String BI_ROUTING_KEY = "bi_routingKey";
    String DEAD_LETTER_QUEUE_ROUTING_KEY="moonbi";
}
