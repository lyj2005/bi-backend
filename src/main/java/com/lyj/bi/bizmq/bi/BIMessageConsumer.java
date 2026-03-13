package com.lyj.bi.bizmq.bi;

import com.lyj.bi.common.ErrorCode;
import com.lyj.bi.exception.BusinessException;
import com.lyj.bi.manager.AiManager;
import com.lyj.bi.model.entity.Chart;
import com.lyj.bi.model.enums.ChartStatusEnum;
import com.lyj.bi.service.ChartService;
import com.lyj.bi.service.UserService;
import com.lyj.bi.utils.ExcelUtils;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class BIMessageConsumer {
    @Resource
    private ChartService chartService;

    @Resource
    private AiManager aiManager;


    @Resource
    private UserService userService;


    /**
     * 接收消息的方法
     *
     * @param message
     * @param channel
     * @param deliveryTag
     */
    @SneakyThrows
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {

        log.info("receiveMessage message = {}", message);

        //1. 处理生产者发来的消息  ---  就是多了这一步和每次失败都要拒绝消息，其他逻辑一样的
        //①如果消息为空。即更新失败了，拒绝消息，重新放到消息队列中
        if (StringUtils.isBlank(message)) {
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"消息为空");
        }
        //②如果图表为空。拒绝消息并抛出异常
        Chart chart = chartService.getById(Long.parseLong(message));
        if (chart == null) {
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表不存在");
        }
        //②如果图表为空。拒绝消息并抛出异常

        //先修改图表任务状态为“执行中”。等执行成功后，修改为“已完成”、保存执行结果；执行失败后，状态修
        //改为“失败”，记录任务失败信息。（为了防止同一个任务被多次执行）
        //2. 修改图表任务状态为“执行中”，提交到数据库
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus("running");
        boolean updateResult = chartService.updateById(updateChart);
        //如果提交失败，数据库出问题了
        if (!updateResult) {
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateException(chart.getId(), "修改图表任务状态为“执行中”失败");
            return;//终止当前任务，注意不是员工  --  线程
        }

//3. 调用AI,得到结果（genChart,genResult）
        String result = aiManager.doChat(buildUserInput(chart));

        //4. 处理结果
        //①依照【【【【【拆分结果，得到字符数组splits
        String[] splits = result.split("【【【【【");
        //②校验
        if (splits.length < 3) {
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateException(chart.getId(), "AI生成错误");
        }
        //③得到（genChart,genResult）,需要去掉多余空格，使用trim方法
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();

        //5. 再次更新数据库
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chart.getId());
        updateChartResult.setGenChart(genChart);
        updateChartResult.setGenResult(genResult);
        updateChartResult.setStatus("succeed");
        boolean b = chartService.updateById(updateChartResult);
        if (!b) {
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateException(chart.getId(), "更新数据库失败");
        }


        //6.  消息确认
        channel.basicAck(deliveryTag, false);
    }


    /**
     * 上面接口很多用到异常
     * 创建一个回调函数，对图表状态失败这一情况进行集中异常处理
     */
    private void handleChartUpdateException(long chartId, String execMessage) {
        //1. 保存到数据库
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setExecMessage(execMessage);
        updateChartResult.setStatus(ChartStatusEnum.FAILED.getValue());
        boolean updateResult = chartService.updateById(updateChartResult);
        //2. 如果更新失败，打日志
        if (!updateResult) {
            log.error("更新图表失败状态失败" + chartId + "," + execMessage);
        }
    }


    /**
     * 构建用户输入方法  --  基于原来的方法改造
     * @param chart
     * @return
     */
    private String buildUserInput(Chart chart) {
            /**（参考）
             分析需求：
             (分析网站用户的增长情况)[，请使用雷达图]
             原始数据：
             (日期，用户数
             1号，10
             2号，20
             3号，30)
             */
            //1. 获取图表信息
        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getChartData();

        //2. 构造用户输入
        StringBuilder userInput = new StringBuilder();
            userInput.append("分析需求:").append("/n");
            //①拼接分析目标
            String userGoal = goal;
            //②拼接图表类型,如果为非空，才拼接在最后
            if (!StringUtils.isEmpty(chartType)) {
                userGoal = userGoal + "，请使用" + chartType;
            }
            userInput.append(userGoal).append("/n");
            //③拼接转换后的图表
            userInput.append("原始数据:").append("/n");
            userInput.append(csvData).append("/n");
            //3. 将stringbuilder转换为string,返回结果
            return userInput.toString();
        }


}
