package com.lyj.bi.bizmq.bi;


/**
 * 指定固定交换机名字  --  因为生产者无具体业务逻辑，所以都是固定的，改一下名字即可  --  优化：直接提取为常量即可
 */
public interface BiMqConstant {

    String BI_EXCHANGE_NAME = "bi_exchange";

    String BI_QUEUE_NAME = "bi_queue";

    String BI_ROUTING_KEY = "bi_routingKey";
}
