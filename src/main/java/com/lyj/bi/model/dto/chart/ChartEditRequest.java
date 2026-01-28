package com.lyj.bi.model.dto.chart;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 编辑请求
 */
@Data
public class ChartEditRequest implements Serializable {

    /**
     * 名称
     */
    private String name;

    private Long id;

    /**
     * 账号
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


    private static final long serialVersionUID = 1L;
}