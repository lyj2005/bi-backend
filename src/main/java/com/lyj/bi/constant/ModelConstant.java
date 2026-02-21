package com.lyj.bi.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.model")
public class ModelConstant {
    /**
     * BI模型ID
     */
    //long BI_MODEL_ID = 1659171950288818178L;
    private Long modelId;
}
