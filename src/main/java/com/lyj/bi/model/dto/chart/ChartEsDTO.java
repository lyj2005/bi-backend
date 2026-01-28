package com.lyj.bi.model.dto.chart;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.util.Date;

/**
 * 帖子 ES 包装类
 **/
// todo 取消注释开启 ES（须先配置 ES）
@Document(indexName = "chart")
@Data
public class ChartEsDTO implements Serializable {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    @Id
    private Long id;
    /**
     * 分析目标
     */
    private String goal;
    /**
     * 图表数据
     */
    private String chartData;
    /**
     * 图表名称
     */
    private String name;
    /**
     * 图表类型
     */
    private String chartType;
    /**
     * 生成的分析结论
     */
    private String genResult;
    /**
     * 任务状态
     */
    private String status;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 创建时间
     */
    @Field(index = false, store = true, type = FieldType.Date, format = {}, pattern = DATE_TIME_PATTERN)
    private Date createTime;
    /**
     * 更新时间
     */
    @Field(index = false, store = true, type = FieldType.Date, format = {}, pattern = DATE_TIME_PATTERN)
    private Date updateTime;
    /**
     * 是否删除
     */
    private Integer isDelete;

}
