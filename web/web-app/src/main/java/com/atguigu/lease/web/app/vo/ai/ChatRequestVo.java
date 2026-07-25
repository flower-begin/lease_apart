package com.atguigu.lease.web.app.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "AI聊天请求")
public class ChatRequestVo {

    @Schema(description = "用户消息")
    private String message;
}
