package com.atguigu.lease.web.app.service.impl;

import com.atguigu.lease.common.context.LoginUserContext;
import com.atguigu.lease.model.entity.*;
import com.atguigu.lease.model.enums.ItemType;
import com.atguigu.lease.model.enums.LeaseStatus;
import com.atguigu.lease.web.app.mapper.*;
import com.atguigu.lease.web.app.service.BrowsingHistoryService;
import com.atguigu.lease.web.app.service.RoomInfoService;
import com.atguigu.lease.web.app.vo.apartment.ApartmentItemVo;
import com.atguigu.lease.web.app.vo.attr.AttrValueVo;
import com.atguigu.lease.web.app.vo.fee.FeeValueVo;
import com.atguigu.lease.web.app.vo.graph.GraphVo;
import com.atguigu.lease.web.app.vo.room.RoomDetailVo;
import com.atguigu.lease.web.app.vo.room.RoomItemVo;
import com.atguigu.lease.web.app.vo.room.RoomQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author liubo
 * @description 针对表【room_info(房间信息表)】的数据库操作Service实现
 * @createDate 2023-07-26 11:12:39
 */
@Service
@Slf4j
public class RoomInfoServiceImpl extends ServiceImpl<RoomInfoMapper, RoomInfo>
        implements RoomInfoService {

    @Autowired
    private RoomInfoMapper roomInfoMapper;

    @Autowired
    private GraphInfoMapper graphInfoMapper;

    @Autowired
    private AttrValueMapper attrValueMapper;

    @Autowired
    private FacilityInfoMapper facilityInfoMapper;

    @Autowired
    private LabelInfoMapper labelInfoMapper;

    @Autowired
    private RoomLabelMapper roomLabelMapper;

    @Autowired
    private PaymentTypeMapper paymentTypeMapper;

    @Autowired
    private FeeValueMapper feeValueMapper;

    @Autowired
    private LeaseTermMapper leaseTermMapper;

    @Autowired
    private ApartmentInfoMapper apartmentInfoMapper;

    @Autowired
    private LeaseAgreementMapper leaseAgreementMapper;

    @Autowired
    private BrowsingHistoryService browsingHistoryService;

    /**
     * 视为"已出租"的租约状态。
     * 注意要和后台房间列表（web-admin RoomInfoMapper.xml 里的 status in (2,5,7)）保持一致，
     * 否则同一个房间在两端会显示不同的入住状态。
     * 「签约待确认」不占用房间——那只是提交了意向，还要管理员确认。
     */
    private static final List<LeaseStatus> OCCUPIED_STATUS =
            List.of(LeaseStatus.SIGNED, LeaseStatus.WITHDRAWING, LeaseStatus.RENEWING);

    @Override
    public void pageItem(Page<RoomItemVo> page, RoomQueryVo queryVo) {
        roomInfoMapper.pageItem(page, queryVo);
        fillRoomItemExtra(page.getRecords());
        fillCheckInFlag(page.getRecords());
    }

    @Override
    public void pageItemByApartmentId(Page<RoomItemVo> page, Long apartmentId) {
        roomInfoMapper.pageItemByApartmentId(page, apartmentId);
        fillRoomItemExtra(page.getRecords());
        fillCheckInFlag(page.getRecords());
    }

    /**
     * 列表页批量补全图片和标签，避免逐条查询（N+1）。
     */
    private void fillRoomItemExtra(List<RoomItemVo> records) {
        if (CollectionUtils.isEmpty(records)) {
            return;
        }
        List<Long> roomIds = records.stream()
                .map(RoomItemVo::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (roomIds.isEmpty()) {
            return;
        }

        // 房间图片
        LambdaQueryWrapper<GraphInfo> graphWrapper = new LambdaQueryWrapper<>();
        graphWrapper.eq(GraphInfo::getItemType, ItemType.ROOM)
                .in(GraphInfo::getItemId, roomIds)
                .orderByAsc(GraphInfo::getId);
        Map<Long, List<GraphVo>> graphMap = graphInfoMapper.selectList(graphWrapper).stream()
                .collect(Collectors.groupingBy(GraphInfo::getItemId,
                        Collectors.mapping(g -> new GraphVo(g.getName(), g.getUrl()), Collectors.toList())));

        // 房间标签
        LambdaQueryWrapper<RoomLabel> roomLabelWrapper = new LambdaQueryWrapper<>();
        roomLabelWrapper.in(RoomLabel::getRoomId, roomIds);
        List<RoomLabel> roomLabels = roomLabelMapper.selectList(roomLabelWrapper);
        Map<Long, List<LabelInfo>> labelMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(roomLabels)) {
            List<Long> labelIds = roomLabels.stream()
                    .map(RoomLabel::getLabelId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (!labelIds.isEmpty()) {
                Map<Long, LabelInfo> labelById = labelInfoMapper.selectBatchIds(labelIds).stream()
                        .collect(Collectors.toMap(LabelInfo::getId, l -> l, (a, b) -> a));
                for (RoomLabel rl : roomLabels) {
                    LabelInfo label = labelById.get(rl.getLabelId());
                    if (label != null) {
                        labelMap.computeIfAbsent(rl.getRoomId(), k -> new ArrayList<>()).add(label);
                    }
                }
            }
        }

        for (RoomItemVo item : records) {
            item.setGraphVoList(graphMap.getOrDefault(item.getId(), Collections.emptyList()));
            item.setLabelInfoList(labelMap.getOrDefault(item.getId(), Collections.emptyList()));
        }
    }

    /**
     * 批量标记房间是否已出租，供列表页展示"已出租"角标。
     */
    private void fillCheckInFlag(List<RoomItemVo> records) {
        if (CollectionUtils.isEmpty(records)) {
            return;
        }
        List<Long> roomIds = records.stream().map(RoomItemVo::getId).filter(Objects::nonNull).toList();
        if (roomIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<LeaseAgreement> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(LeaseAgreement::getRoomId, roomIds);
        wrapper.in(LeaseAgreement::getStatus, OCCUPIED_STATUS);
        Set<Long> occupied = leaseAgreementMapper.selectList(wrapper).stream()
                .map(LeaseAgreement::getRoomId)
                .collect(Collectors.toSet());
        for (RoomItemVo item : records) {
            item.setIsCheckIn(occupied.contains(item.getId()));
        }
    }

    @Override
    public RoomDetailVo getDetailById(Long id) {
        RoomInfo roomInfo = getById(id);
        if (roomInfo == null) {
            return null;
        }

        ApartmentInfo apartmentInfo = apartmentInfoMapper.selectById(roomInfo.getApartmentId());

        List<GraphVo> graphVoList = graphInfoMapper.customQueryList(ItemType.ROOM, id);
        List<AttrValueVo> attrValueVoList = attrValueMapper.customQueryList(id);
        List<FacilityInfo> facilityInfoList = facilityInfoMapper.customQueryListByRoomId(id);
        List<LabelInfo> labelInfoList = labelInfoMapper.customQueryListByRoomId(id);
        List<PaymentType> paymentTypeList = paymentTypeMapper.customQueryList(id);
        List<FeeValueVo> feeValueVoList = feeValueMapper.customQueryList(roomInfo.getApartmentId());
        List<LeaseTerm> leaseTermList = leaseTermMapper.customQueryList(id);

        RoomDetailVo roomDetailVo = new RoomDetailVo();
        BeanUtils.copyProperties(roomInfo, roomDetailVo);
        roomDetailVo.setGraphVoList(graphVoList);
        roomDetailVo.setAttrValueVoList(attrValueVoList);
        roomDetailVo.setFacilityInfoList(facilityInfoList);
        roomDetailVo.setLabelInfoList(labelInfoList);
        roomDetailVo.setPaymentTypeList(paymentTypeList);
        roomDetailVo.setFeeValueVoList(feeValueVoList);
        roomDetailVo.setLeaseTermList(leaseTermList);
        roomDetailVo.setIsCheckIn(isCheckIn(id));

        if (apartmentInfo != null) {
            ApartmentItemVo apartmentItemVo = new ApartmentItemVo();
            BeanUtils.copyProperties(apartmentInfo, apartmentItemVo);
            apartmentItemVo.setGraphVoList(graphInfoMapper.customQueryList(ItemType.APARTMENT, apartmentInfo.getId()));
            roomDetailVo.setApartmentItemVo(apartmentItemVo);
        }

        recordBrowsingHistory(id);

        return roomDetailVo;
    }

    /**
     * 房间是否已出租：存在生效中的租约即为已租。
     */
    private Boolean isCheckIn(Long roomId) {
        LambdaQueryWrapper<LeaseAgreement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LeaseAgreement::getRoomId, roomId);
        wrapper.in(LeaseAgreement::getStatus, OCCUPIED_STATUS);
        return leaseAgreementMapper.selectCount(wrapper) > 0;
    }

    /**
     * 记录浏览历史。同一用户对同一房间只保留一条，重复浏览只更新时间。
     */
    private void recordBrowsingHistory(Long roomId) {
        Long userId = LoginUserContext.getUserId();
        if (userId == null) {
            return;
        }
        try {
            LambdaQueryWrapper<BrowsingHistory> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BrowsingHistory::getUserId, userId);
            wrapper.eq(BrowsingHistory::getRoomId, roomId);
            BrowsingHistory exists = browsingHistoryService.getOne(wrapper, false);
            if (exists != null) {
                exists.setBrowseTime(new Date());
                browsingHistoryService.updateById(exists);
            } else {
                BrowsingHistory history = new BrowsingHistory();
                history.setUserId(userId);
                history.setRoomId(roomId);
                history.setBrowseTime(new Date());
                browsingHistoryService.save(history);
            }
        } catch (Exception e) {
            log.warn("记录浏览历史失败, roomId={}: {}", roomId, e.getMessage());
        }
    }
}
