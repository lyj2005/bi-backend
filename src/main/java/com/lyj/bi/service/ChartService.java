package com.lyj.bi.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lyj.bi.model.dto.chart.ChartQueryRequest;
import com.lyj.bi.model.dto.chart.GenChartByAiRequest;
import com.lyj.bi.model.entity.Chart;
import com.lyj.bi.model.entity.User;
import com.lyj.bi.model.vo.BiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.util.List;

public interface ChartService extends IService<Chart> {



    /**
     * ai调用次数统计
     * @param chartId
     * @return
     */
    Integer genChartByAICount(Long chartId);

}
