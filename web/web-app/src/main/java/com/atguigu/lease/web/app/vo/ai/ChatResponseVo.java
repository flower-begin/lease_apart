package com.atguigu.lease.web.app.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "AI聊天响应")
public class ChatResponseVo {

    @Schema(description = "AI回复消息")
    private String message;

    @Schema(description = "推荐的公寓ID列表")
    private List<Long> recommendedApartmentIds;
}
