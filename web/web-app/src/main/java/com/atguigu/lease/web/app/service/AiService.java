package com.atguigu.lease.web.app.service;

import com.atguigu.lease.web.app.vo.ai.ChatRequestVo;
import com.atguigu.lease.web.app.vo.ai.ChatResponseVo;
import com.atguigu.lease.web.app.vo.ai.RecommendRequestVo;
import com.atguigu.lease.web.app.vo.apartment.ApartmentItemVo;

import java.util.List;

public interface AiService {

    ChatResponseVo chat(ChatRequestVo request);

    List<ApartmentItemVo> recommend(RecommendRequestVo request);
}
