package com.lyj.bi.biMqConfig;

import com.lyj.bi.common.ErrorCode;
import com.lyj.bi.componet.WebSocketServer;
import com.lyj.bi.constant.ModelConstant;
import com.lyj.bi.exception.BusinessException;
import com.lyj.bi.manager.AiManager;
import com.lyj.bi.model.entity.Chart;
import com.lyj.bi.model.entity.User;
import com.lyj.bi.model.enums.ChartStatusEnum;
import com.lyj.bi.service.ChartService;
import com.lyj.bi.service.UserService;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.aop.framework.AopContext;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.lyj.bi.MQconstant.BiMqContant.BI_QUEUE;


@Component
@Slf4j
public class BIMessageConsumer {
    @Resource
    private ChartService chartService;

    @Resource
    private AiManager aiManager;

    @Resource
    private ModelConstant modelConstant;

    @Resource
    private UserService userService;

    @Resource
    private WebSocketServer webSocketService;

    /**
     * 接收消息的方法
     *
     * @param message
     * @param channel
     * @param deliveryTag
     */
    //使用@SneakyThrows注解简化异常处理
    @SneakyThrows
    //使用该注解指定程序要监听的队列，并设置消息的确认机制为手动
    @RabbitListener(queues = {BI_QUEUE}, ackMode = "MANUAL",autoStartup = "false")
    //@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag 用于从消息头中获取投递标签deliveryTag
    //在mq中，每条消息都会被分配一个唯一投递标签，用于标识该消息在通道中的投递状态和顺序，使用该注解可以从消息头中获取该投递标签，并将其赋值给deliveryTag参数，
    @Transactional
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {

        if (StringUtils.isBlank(message)) {
            //如果消息为空，拒绝消息
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息为空");
        }
        log.info("消息队列收到消息：{}", message);
        long chartId = Long.parseLong(message);
        Chart chart = chartService.getById(chartId);
        if (chart == null) {
            //拒绝
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图表不存在");
        }

        //先修改图表任务状态为”执行中”。等执行成功后，修改为“已完成”、保存执行结果；执行失败后，状态修改为“失败”，记录任务失败信息。
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus(ChartStatusEnum.RUNNING.getValue());
        boolean b = chartService.updateById(updateChart);
        if (!b) {
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateError(chart.getId(), "更新图表执行中状态失败");
            return;
        }
        //判断AI接口的调用次数
        Long userId = chart.getUserId();
        //获取用户信息
        User user = userService.getById(userId);
        Integer number = user.getNumber();
        //判断AI接口调用次数是否大于0
        if (number <= 0) {
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateError(chart.getId(), "AI调用次数已耗尽！");
            return;
        }
        //AI调用次数扣减
        ChartService currentProxy = (ChartService) AopContext.currentProxy();
        Integer chartByAICount = currentProxy.genChartByAICount(chart.getUserId());
        //判断AI接口调用次数是否扣减失败
        if (chartByAICount.equals(1)) {
            channel.basicNack(deliveryTag, false, false);
        }
        //获取🐟聪明AI模型ID号
        long bimodelId=modelConstant.getModelId();
        // 调用AI,aiManager的doChart方法，传入biModelID和userInput
        //String result = aiManager.doChart(bimodelId, bulidUserInput(chart));

        //调用星火AI
        //分析对象 = 预设 + 分析需求（goal） + 原始数据（cvsData）+ 图表类型（chartType）
        //String promote = SYSTEM_PROMPT+bulidUserInput(chart);
        String result = aiManager.doChat(bimodelId,bulidUserInput(chart));

        // 将返回结果按"【【【【【"分割
        String[] splits = result.split("【【【【【");
        // 如果分割后的结果长度小于3，抛出异常
        if (splits.length < 3) {
            channel.basicNack(deliveryTag, false, false);
            userService.refundAICount(userId); // 回补调用次数
            handleChartUpdateError(chart.getId(), "AI 生成错误");
        }
        // 获取生成的图表和结果
        String genChart = splits[1].trim();
        String genResult = splits[2].trim();
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chart.getId());
        updateChartResult.setGenChart(genChart);
        updateChartResult.setGenResult(genResult);
        updateChartResult.setStatus(ChartStatusEnum.SUCCEED.getValue());
        boolean updateResult = chartService.updateById(updateChartResult);
        if (!updateResult) {
            channel.basicNack(deliveryTag, false, false);
            userService.refundAICount(userId); // 回补调用次数
            handleChartUpdateError(chart.getId(), "更新图表成功状态失败");
        }
        webSocketService.sendOneMessage(userId,"图表生成成功！");
    }


    /**
     * 构建用户输入
     *
     * @param chart
     * @return
     */
    public String bulidUserInput(Chart chart) {
        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getChartData();

        //构造用户输入
        StringBuilder userInput = new StringBuilder();
        //TODO 🐟AI设置模型预设，星火AI没有，所以这里需要加上这段代码
        userInput.append(AiManager.SYSTEM_PROMPT);

        userInput.append("分析需求：").append("\n");

        //拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据:").append("\n");
        // 将csv文件内容添加到userInput中
        userInput.append(csvData).append("\n");
        return userInput.toString();
    }

    /**
     * 上面接口很多用到异常
     * 创建一个回调函数，对图表状态失败这一情况进行集中异常处理
     */
    private void handleChartUpdateError(long chartId, String execMessage) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setExecMessage(execMessage);
        updateChartResult.setStatus(ChartStatusEnum.FAILED.getValue());
        boolean updateResult = chartService.updateById(updateChartResult);
        if (!updateResult) {
            webSocketService.sendAllMessage("图表生成失败！");
            log.error("更新图表失败状态失败" + chartId + "," + execMessage);
        }
    }
}
