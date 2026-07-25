package com.atguigu.lease.web.app.controller.ai;

import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.web.app.service.AiService;
import com.atguigu.lease.web.app.vo.ai.ChatRequestVo;
import com.atguigu.lease.web.app.vo.ai.ChatResponseVo;
import com.atguigu.lease.web.app.vo.ai.RecommendRequestVo;
import com.atguigu.lease.web.app.vo.apartment.ApartmentItemVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "AI助手")
@RequestMapping("/app/ai")
public class AiChatController {

    @Autowired
    private AiService aiService;

    @PostMapping("chat")
    @Operation(summary = "AI聊天")
    public Result<ChatResponseVo> chat(@RequestBody ChatRequestVo request) {
        return Result.ok(aiService.chat(request));
    }

    @PostMapping("recommend")
    @Operation(summary = "AI推荐公寓")
    public Result<List<ApartmentItemVo>> recommend(@RequestBody RecommendRequestVo request) {
        return Result.ok(aiService.recommend(request));
    }
}
