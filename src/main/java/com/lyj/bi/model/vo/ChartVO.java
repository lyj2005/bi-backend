package com.lyj.bi.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ChartVO implements Serializable {

    /**
     * id
     */
    private long id;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表数据
     */
    private String chartData;

    /**
     * 图表类型
     */
    private String chartType;

    /**
     * 生成的图表数据
     */
    private String genChart;

    /**
     * 生成的分析结论
     */
    private String genResult;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * wait,running,succeed,failed
     */
    private String status;

    /**
     * 错误消息
     */
    private String execMessage;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private static final long serialVersionUID = 4377216422203918656L;


}