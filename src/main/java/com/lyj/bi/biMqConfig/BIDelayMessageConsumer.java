package com.lyj.bi.biMqConfig;

import com.lyj.bi.common.ErrorCode;
import com.lyj.bi.exception.BusinessException;
import com.lyj.bi.model.entity.Chart;
import com.lyj.bi.model.enums.ChartStatusEnum;
import com.lyj.bi.service.ChartService;
import com.rabbitmq.client.Channel;

import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import static com.lyj.bi.MQconstant.BiMqContant.BI_DEAD_QUEUE;


/**
 * 延迟队列消费者
 */
//@Component
@Slf4j
public class BIDelayMessageConsumer {
    @Resource
    private ChartService chartService;


    /**
     * 接收消息的方法
     *
     * @param message
     * @param channel
     * @param deliveryTag
     */
    //使用@SneakyThrows注解简化异常处理
    @SneakyThrows
    //使用该注解指定程序要监听的死信队列，并设置消息的确认机制为手动
    @RabbitListener(queues = {BI_DEAD_QUEUE}, ackMode = "MANUAL",autoStartup = "false")
    //@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag 用于从消息头中获取投递标签deliveryTag
    //在mq中，每条消息都会被分配一个唯一投递标签，用于标识该消息在通道中的投递状态和顺序，使用该注解可以从消息头中获取该投递标签，并将其赋值给deliveryTag参数，
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {

        if (StringUtils.isBlank(message)) {
            //如果消息为空，拒绝消息
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息为空");
        }
        long chartId = Long.parseLong(message);
        log.info("延迟队列接收到消息，图表id为{}", chartId);
        Chart chart = chartService.getById(chartId);
        if (chart == null) {
            //拒绝
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图表不存在");
        }
        //获取图表信息
        String status = chart.getStatus();
        //如果图表状态还是失败和等待超时就删除
        if (ChartStatusEnum.FAILED.getValue().equals(status)|| ChartStatusEnum.WAITING.getValue().equals(status)) {
            channel.basicAck(deliveryTag, false);
            chartService.removeById(chartId);
            log.info("id为{}的图表已被删除成功,延迟队列任务圆满完成！😄", chartId);
        }
    }
}
