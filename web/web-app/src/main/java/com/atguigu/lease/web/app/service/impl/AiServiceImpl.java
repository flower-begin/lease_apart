package com.atguigu.lease.web.app.service.impl;

import com.atguigu.lease.model.entity.ApartmentInfo;
import com.atguigu.lease.model.entity.RoomInfo;
import com.atguigu.lease.web.app.custom.config.DeepSeekConfigProperties;
import com.atguigu.lease.web.app.service.AiService;
import com.atguigu.lease.web.app.service.ApartmentInfoService;
import com.atguigu.lease.web.app.service.RoomInfoService;
import com.atguigu.lease.web.app.vo.ai.ChatRequestVo;
import com.atguigu.lease.web.app.vo.ai.ChatResponseVo;
import com.atguigu.lease.web.app.vo.ai.RecommendRequestVo;
import com.atguigu.lease.web.app.vo.apartment.ApartmentItemVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiServiceImpl implements AiService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DeepSeekConfigProperties deepSeekConfig;

    @Autowired
    private ApartmentInfoService apartmentInfoService;

    @Autowired
    private RoomInfoService roomInfoService;

    @Override
    public ChatResponseVo chat(ChatRequestVo request) {
        // Build database context for the AI
        String dbContext = buildApartmentContext();

        // Build system prompt
        String systemPrompt = "你是一个专业的公寓租赁助手，可以帮助用户了解公寓信息、推荐合适的房源。" +
                "请根据提供的公寓数据上下文来回答用户问题。如果用户询问的信息不在上下文中，请礼貌地告知。" +
                "回答时请使用友好、专业的语气，并用中文回复。";

        // Call DeepSeek API
        String response = callDeepSeek(systemPrompt, "以下是当前可用的公寓信息：\n\n" + dbContext + "\n\n用户问题：" + request.getMessage());

        ChatResponseVo vo = new ChatResponseVo();
        vo.setMessage(response);
        vo.setRecommendedApartmentIds(Collections.emptyList());
        return vo;
    }

    @Override
    public List<ApartmentItemVo> recommend(RecommendRequestVo request) {
        // Query apartments based on preference
        LambdaQueryWrapper<ApartmentInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApartmentInfo::getIsRelease, 1);
        List<ApartmentInfo> apartments = apartmentInfoService.list(wrapper);

        // Build context for AI ranking
        StringBuilder contextBuilder = new StringBuilder();
        for (ApartmentInfo apt : apartments) {
            contextBuilder.append(String.format("- ID:%d, 名称:%s, 位置:%s%s%s, 地址:%s\n",
                    apt.getId(), apt.getName(),
                    apt.getProvinceName(), apt.getCityName(), apt.getDistrictName(),
                    apt.getAddressDetail()));
        }

        String systemPrompt = "你是一个公寓推荐助手。根据用户的偏好，从提供的公寓列表中推荐最合适的3-5个公寓。" +
                "只返回推荐的公寓ID列表，格式为JSON数组，例如：[1,2,3]。不要返回其他内容。";

        String response = callDeepSeek(systemPrompt, "用户偏好：" + request.getPreference() + "\n\n" + contextBuilder.toString());

        // Parse apartment IDs from response
        List<Long> recommendedIds = parseApartmentIds(response);

        // Build result
        List<ApartmentItemVo> results = new ArrayList<>();
        for (Long id : recommendedIds) {
            ApartmentInfo apt = apartmentInfoService.getById(id);
            if (apt != null) {
                ApartmentItemVo vo = new ApartmentItemVo();
                org.springframework.beans.BeanUtils.copyProperties(apt, vo);
                results.add(vo);
            }
        }

        return results;
    }

    private String buildApartmentContext() {
        LambdaQueryWrapper<ApartmentInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApartmentInfo::getIsRelease, 1);
        List<ApartmentInfo> apartments = apartmentInfoService.list(wrapper);

        StringBuilder sb = new StringBuilder();
        for (ApartmentInfo apt : apartments) {
            // Get room count and min rent for this apartment
            LambdaQueryWrapper<RoomInfo> roomWrapper = new LambdaQueryWrapper<>();
            roomWrapper.eq(RoomInfo::getApartmentId, apt.getId());
            roomWrapper.eq(RoomInfo::getIsRelease, 1);
            List<RoomInfo> rooms = roomInfoService.list(roomWrapper);

            BigDecimal minRent = rooms.stream()
                    .map(RoomInfo::getRent)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            int roomCount = rooms.size();

            sb.append(String.format("公寓: %s, 位置: %s%s%s, 地址: %s, 房间数: %d, 最低月租: %.0f元\n",
                    apt.getName(),
                    apt.getProvinceName() != null ? apt.getProvinceName() : "",
                    apt.getCityName() != null ? apt.getCityName() : "",
                    apt.getDistrictName() != null ? apt.getDistrictName() : "",
                    apt.getAddressDetail(),
                    roomCount,
                    minRent));
        }
        return sb.toString();
    }

    private String callDeepSeek(String systemPrompt, String userMessage) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(deepSeekConfig.getKey());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", deepSeekConfig.getModel());

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);

            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(deepSeekConfig.getUrl(), entity, String.class);

            // Parse OpenAI-compatible response
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            log.error("DeepSeek API call failed", e);
            return "抱歉，AI服务暂时不可用，请稍后再试。错误信息：" + e.getMessage();
        }
    }

    private List<Long> parseApartmentIds(String response) {
        try {
            // Try to extract JSON array from response
            String trimmed = response.trim();
            if (trimmed.startsWith("```")) {
                // Remove markdown code blocks
                trimmed = trimmed.replaceAll("```json|```", "").trim();
            }
            if (trimmed.startsWith("[")) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode array = mapper.readTree(trimmed);
                List<Long> ids = new ArrayList<>();
                for (JsonNode node : array) {
                    ids.add(node.asLong());
                }
                return ids;
            }
        } catch (Exception e) {
            log.warn("Failed to parse apartment IDs from AI response: {}", response);
        }
        return Collections.emptyList();
    }
}
