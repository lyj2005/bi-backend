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
     * 智能数据分析生成
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param loginUser
     * @return
     * @throws FileNotFoundException
     */
    BiResponse genChartByAiService(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest,
                                   User loginUser) throws FileNotFoundException;

    /**
     * AI模型智能分析
     * @param multipartFile
     * @param genChartByAiRequest
     * @param loginUser
     * @return
     * @throws FileNotFoundException
     */
    BiResponse genChartBySparkAiService(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, User loginUser) throws FileNotFoundException;

    /**
     * 智能分析（异步线程池）
     * @param multipartFile
     * @param genChartByAiRequest
     * @param loginUser
     * @return
     */
    BiResponse genChartByAiAsycnService(MultipartFile multipartFile,
                                        GenChartByAiRequest genChartByAiRequest, User loginUser) throws FileNotFoundException;

    /**
     * 智能分析（异步消息队列）
     * @param multipartFile
     * @param genChartByAiRequest
     * @param loginUser
     * @return
     */
    BiResponse genChartByAiMQService(MultipartFile multipartFile,
                                     GenChartByAiRequest genChartByAiRequest, User loginUser) throws FileNotFoundException;

    /**
     * 图表列表信息缓存
     * @return
     */
    List<Chart> listChartByCacheService();

    /**
     * 图表列表信息分页缓存
     * @return
     */
    List<Chart> listChartByCachePage(ChartQueryRequest chartQueryRequest,
                                     HttpServletRequest request);

    /**
     * 获取单个图表缓存
     * @param id
     * @return
     */
    Chart getChartByIdCache(long id);

    /**
     * ai调用次数统计
     * @param chartId
     * @return
     */
    Integer genChartByAICount(Long chartId);

}
