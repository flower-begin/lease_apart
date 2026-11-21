package com.atguigu.lease.web.app.service.impl;

import com.atguigu.lease.model.entity.ApartmentInfo;
import com.atguigu.lease.model.entity.RoomInfo;
import com.atguigu.lease.model.enums.ItemType;
import com.atguigu.lease.model.enums.ReleaseStatus;
import com.atguigu.lease.web.app.custom.config.DeepSeekConfigProperties;
import com.atguigu.lease.web.app.mapper.GraphInfoMapper;
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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @Autowired
    private GraphInfoMapper graphInfoMapper;

    /** AI 在回答末尾追加的推荐 id 标记：<IDS>1,2,3</IDS> */
    private static final Pattern IDS_TAG = Pattern.compile("<IDS>([\\d,\\s]*)</IDS>", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ChatResponseVo chat(ChatRequestVo request) {
        if (request == null || !StringUtils.hasText(request.getMessage())) {
            ChatResponseVo vo = new ChatResponseVo();
            vo.setMessage("请先输入你想咨询的问题～");
            vo.setRecommendedApartmentIds(Collections.emptyList());
            return vo;
        }

        String dbContext = buildApartmentContext();

        String systemPrompt = "你是一个专业的公寓租赁助手，可以帮助用户了解公寓信息、推荐合适的房源。" +
                "请根据下面提供的公寓数据上下文回答用户问题；上下文里没有的信息请礼貌说明你不清楚。" +
                "用友好、专业的中文回答，可以使用 Markdown 让排版更清晰（列表、加粗）。" +
                "如果你在回答中推荐了某些公寓，请在回答的最后单独追加一行：<IDS>公寓ID1,公寓ID2</IDS>，" +
                "其中 ID 只能取自上下文里出现过的“ID:数字”。不要推荐列表之外的公寓。";

        String response = callDeepSeek(systemPrompt,
                "以下是当前可用的公寓信息：\n\n" + dbContext + "\n\n用户问题：" + request.getMessage());

        ChatResponseVo vo = new ChatResponseVo();
        // 把 <IDS> 标记提取出来并从正文里去掉
        List<Long> ids = Collections.emptyList();
        if (response != null) {
            Matcher matcher = IDS_TAG.matcher(response);
            if (matcher.find()) {
                ids = parseIdList(matcher.group(1));
                response = response.replace(matcher.group(0), "").trim();
            }
        }
        vo.setMessage(response);
        vo.setRecommendedApartmentIds(ids);
        return vo;
    }

    @Override
    public List<ApartmentItemVo> recommend(RecommendRequestVo request) {
        String preference = request == null ? null : request.getPreference();
        Long cityId = request == null ? null : request.getCityId();
        BigDecimal maxRent = request == null ? null : request.getMaxRent();

        // 先按规则筛出候选集，AI 只在这个集合里排序，避免推荐出不存在的公寓
        List<ApartmentInfo> candidates = filterApartments(cityId, maxRent);
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> recommendedIds = Collections.emptyList();
        if (StringUtils.hasText(preference)) {
            StringBuilder contextBuilder = new StringBuilder();
            for (ApartmentInfo apt : candidates) {
                contextBuilder.append(String.format("- ID:%d, 名称:%s, 位置:%s%s%s, 地址:%s, 最低月租:%s元\n",
                        apt.getId(), apt.getName(),
                        nullToEmpty(apt.getProvinceName()), nullToEmpty(apt.getCityName()),
                        nullToEmpty(apt.getDistrictName()), nullToEmpty(apt.getAddressDetail()),
                        minRentOf(apt.getId())));
            }

            String systemPrompt = "你是一个公寓推荐助手。根据用户偏好，从给定的候选公寓中挑选最合适的 3~5 个。" +
                    "只返回 JSON 数组形式的公寓 ID，例如 [1,2,3]，不要输出任何其他文字。";
            String response = callDeepSeek(systemPrompt,
                    "用户偏好：" + preference + "\n\n候选公寓：\n" + contextBuilder);
            recommendedIds = parseApartmentIds(response);
        }

        List<ApartmentInfo> chosen = new ArrayList<>();
        if (!recommendedIds.isEmpty()) {
            Map<Long, ApartmentInfo> byId = new HashMap<>();
            for (ApartmentInfo apt : candidates) {
                byId.put(apt.getId(), apt);
            }
            for (Long id : recommendedIds) {
                ApartmentInfo apt = byId.get(id);
                if (apt != null) {
                    chosen.add(apt);
                }
            }
        }
        // AI 没给出结果（或返回了无效 id）时退化为规则排序
        if (chosen.isEmpty()) {
            chosen = candidates;
        }

        return chosen.stream().limit(5).map(this::toItemVo).toList();
    }

    /**
     * 规则候选集：已发布 + 可选城市 + 可选租金上限，按最低租金升序。
     */
    private List<ApartmentInfo> filterApartments(Long cityId, BigDecimal maxRent) {
        LambdaQueryWrapper<ApartmentInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApartmentInfo::getIsRelease, ReleaseStatus.RELEASED);
        if (cityId != null) {
            wrapper.eq(ApartmentInfo::getCityId, cityId);
        }
        List<ApartmentInfo> apartments = apartmentInfoService.list(wrapper);
        if (maxRent == null) {
            return apartments;
        }
        return apartments.stream()
                .filter(apt -> {
                    BigDecimal min = minRentOf(apt.getId());
                    return min != null && min.compareTo(maxRent) <= 0;
                })
                .sorted(Comparator.comparing(apt -> {
                    BigDecimal min = minRentOf(apt.getId());
                    return min == null ? BigDecimal.valueOf(Long.MAX_VALUE) : min;
                }))
                .toList();
    }

    private ApartmentItemVo toItemVo(ApartmentInfo apartmentInfo) {
        ApartmentItemVo vo = new ApartmentItemVo();
        BeanUtils.copyProperties(apartmentInfo, vo);
        vo.setGraphVoList(graphInfoMapper.customQueryList(ItemType.APARTMENT, apartmentInfo.getId()));
        vo.setMinRent(minRentOf(apartmentInfo.getId()));
        return vo;
    }

    private BigDecimal minRentOf(Long apartmentId) {
        LambdaQueryWrapper<RoomInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoomInfo::getApartmentId, apartmentId);
        wrapper.eq(RoomInfo::getIsRelease, ReleaseStatus.RELEASED);
        return roomInfoService.list(wrapper).stream()
                .map(RoomInfo::getRent)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);
    }

    private String buildApartmentContext() {
        LambdaQueryWrapper<ApartmentInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApartmentInfo::getIsRelease, ReleaseStatus.RELEASED);
        List<ApartmentInfo> apartments = apartmentInfoService.list(wrapper);

        if (apartments.isEmpty()) {
            return "（当前还没有已发布的公寓）";
        }

        StringBuilder sb = new StringBuilder();
        for (ApartmentInfo apt : apartments) {
            LambdaQueryWrapper<RoomInfo> roomWrapper = new LambdaQueryWrapper<>();
            roomWrapper.eq(RoomInfo::getApartmentId, apt.getId());
            roomWrapper.eq(RoomInfo::getIsRelease, ReleaseStatus.RELEASED);
            List<RoomInfo> rooms = roomInfoService.list(roomWrapper);

            BigDecimal minRent = rooms.stream()
                    .map(RoomInfo::getRent)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            sb.append(String.format(
                    "ID:%d, 公寓: %s, 位置: %s%s%s, 地址: %s, 房间数: %d, 最低月租: %.0f元, 电话: %s\n",
                    apt.getId(),
                    apt.getName(),
                    nullToEmpty(apt.getProvinceName()),
                    nullToEmpty(apt.getCityName()),
                    nullToEmpty(apt.getDistrictName()),
                    nullToEmpty(apt.getAddressDetail()),
                    rooms.size(),
                    minRent,
                    nullToEmpty(apt.getPhone())));
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
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userMessage));

            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(deepSeekConfig.getUrl(), entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new IllegalStateException("AI 返回格式异常：" + response.getBody());
            }
            return choices.get(0).path("message").path("content").asText();
        } catch (Exception e) {
            log.error("DeepSeek API 调用失败", e);
            return "抱歉，AI 助手暂时连不上，请稍后再试。你也可以直接在「找房」里按地区和价格筛选房源。";
        }
    }

    private List<Long> parseApartmentIds(String response) {
        if (!StringUtils.hasText(response)) {
            return Collections.emptyList();
        }
        try {
            String trimmed = response.trim().replaceAll("```json|```", "").trim();
            int start = trimmed.indexOf('[');
            int end = trimmed.lastIndexOf(']');
            if (start >= 0 && end > start) {
                JsonNode array = objectMapper.readTree(trimmed.substring(start, end + 1));
                List<Long> ids = new ArrayList<>();
                for (JsonNode node : array) {
                    if (node.isNumber()) {
                        ids.add(node.asLong());
                    } else if (node.isTextual()) {
                        ids.add(Long.parseLong(node.asText().replaceAll("\\D", "")));
                    }
                }
                return ids;
            }
        } catch (Exception e) {
            log.warn("解析 AI 返回的公寓 id 失败: {}", response);
        }
        return Collections.emptyList();
    }

    private List<Long> parseIdList(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : raw.split(",")) {
            String digits = part.replaceAll("\\D", "");
            if (!digits.isEmpty()) {
                ids.add(Long.parseLong(digits));
            }
        }
        return ids;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
