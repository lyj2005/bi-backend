package com.lyj.bi.manager;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.lyj.bi.common.ErrorCode;
import com.lyj.bi.exception.BusinessException;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.lkeap.v20240522.LkeapClient;
import com.tencentcloudapi.lkeap.v20240522.models.ChatCompletionsRequest;
import com.tencentcloudapi.lkeap.v20240522.models.ChatCompletionsResponse;
import com.tencentcloudapi.lkeap.v20240522.models.Message;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;


/**
 * 用于对接 AI 平台
 */
@Service
@Slf4j
public class AiManager {


    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${deepseek.api.base-url}")
    private String baseUrl;

    @Value("${deepseek.api.model}")
    private String model;

    public static final String SYSTEM_PROMPT = "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
            "分析需求：\n" +
            "{数据分析的需求或者目标}\n" +
            "原始数据：\n" +
            "{csv格式的原始数据，用,作为分隔符}\n" +
            "请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
            "【【【【【\n" +
            "{前端 Echarts V5 的 option 配置对象js代码（输出 json 格式），合理地将数据进行可视化，不要生成任何多余的内容，比如注释}\n" +
            "【【【【【\n" +
            "{明确的数据分析结论、越详细越好，不要生成多余的注释}\n" +
            "【【【【【";


    /**
     * AI 对话
     *
     * @param modelId
     * @param message
     * @return
     */
    public String doChat(long modelId, String message) {
        // 写系统预设
        final String SYSTEM_PROMPT = "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
                "分析需求：\n" +
                "{数据分析的需求或者目标}\n" +
                "原始数据：\n" +
                "{csv格式的原始数据，用,作为分隔符}\n" +
                "请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
                "【【【【【\n" +
                "{前端 Echarts V5 的 option 配置对象js代码（输出 json 格式），合理地将数据进行可视化，不要生成任何多余的内容，比如注释}\n" +
                "【【【【【\n" +
                "{明确的数据分析结论、越详细越好，不要生成多余的注释}\n" +
                "【【【【【";

        try {
            // 构建请求URL
            String url = baseUrl + "/chat/completions";

            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);
            requestBody.put("messages", new JSONArray()
                    .put(new JSONObject()
                            .put("role", "user")
                            .put("content", SYSTEM_PROMPT+"内容如下："+message)
                    )
            );
            requestBody.put("max_tokens", 2000);
            requestBody.put("temperature", 0.7);
            requestBody.put("stream", false);

            // 创建HTTP请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            // 发送请求
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 检查响应状态
            if (response.statusCode() != 200) {
                log.error("DeepSeek API 响应错误: {}", response.body());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 服务调用失败");
            }

            // 解析响应
            return parseAiResponse(response.body());

        } catch (Exception e) {
            log.error("调用DeepSeek AI失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 服务调用失败");
        }




    }

    /**
     * 解析AI响应
     */
    private String parseAiResponse(String responseBody) {
        try {
            JSONObject jsonResponse = new JSONObject(responseBody);
            JSONArray choices = jsonResponse.getJSONArray("choices");
            if (choices.size() > 0) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                return message.getStr("content");
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 返回结果为空");
        } catch (Exception e) {
            log.error("解析AI响应失败: {}", responseBody, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 响应解析失败");
        }
    }

}
