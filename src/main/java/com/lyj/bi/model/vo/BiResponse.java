package com.lyj.bi.model.vo;

import lombok.Data;

/**
 * BI的返回结果
 */
@Data
public class BiResponse {
    private String genResult;

    private String genChart;

    private Long ChartId;
}
