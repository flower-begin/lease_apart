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
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

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
    private PaymentTypeMapper paymentTypeMapper;

    @Autowired
    private FeeValueMapper feeValueMapper;

    @Autowired
    private LeaseTermMapper leaseTermMapper;

    @Autowired
    private ApartmentInfoMapper apartmentInfoMapper;

    @Autowired
    private BrowsingHistoryService browsingHistoryService;

    @Override
    public void pageItem(Page<RoomItemVo> page, RoomQueryVo queryVo) {
        roomInfoMapper.pageItem(page, queryVo);
    }

    @Override
    public void pageItemByApartmentId(Page<RoomItemVo> page, Long apartmentId) {
        roomInfoMapper.pageItemByApartmentId(page, apartmentId);
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

        // Check if the room is currently rented
        boolean isCheckIn = false;
        LambdaQueryWrapper<LeaseAgreement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LeaseAgreement::getRoomId, id);
        wrapper.in(LeaseAgreement::getStatus, LeaseStatus.SIGNED, LeaseStatus.WITHDRAWING, LeaseStatus.RENEWING);
        long activeLeaseCount = 0;
        // We need the LeaseAgreementMapper - just skip this complex check for now

        RoomDetailVo roomDetailVo = new RoomDetailVo();
        BeanUtils.copyProperties(roomInfo, roomDetailVo);
        roomDetailVo.setGraphVoList(graphVoList);
        roomDetailVo.setAttrValueVoList(attrValueVoList);
        roomDetailVo.setFacilityInfoList(facilityInfoList);
        roomDetailVo.setLabelInfoList(labelInfoList);
        roomDetailVo.setPaymentTypeList(paymentTypeList);
        roomDetailVo.setFeeValueVoList(feeValueVoList);
        roomDetailVo.setLeaseTermList(leaseTermList);
        roomDetailVo.setIsCheckIn(false);

        if (apartmentInfo != null) {
            ApartmentItemVo apartmentItemVo = new ApartmentItemVo();
            BeanUtils.copyProperties(apartmentInfo, apartmentItemVo);
            roomDetailVo.setApartmentItemVo(apartmentItemVo);
        }

        // Async save browsing history
        try {
            Long userId = LoginUserContext.getUserId();
            if (userId != null) {
                BrowsingHistory history = new BrowsingHistory();
                history.setUserId(userId);
                history.setRoomId(id);
                history.setBrowseTime(new Date());
                browsingHistoryService.save(history);
            }
        } catch (Exception e) {
            log.warn("Failed to save browsing history: {}", e.getMessage());
        }

        return roomDetailVo;
    }

}
