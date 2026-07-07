package com.atguigu.lease.web.admin.service.impl;

import com.atguigu.lease.common.exception.LeaseException;
import com.atguigu.lease.common.result.ResultCodeEnum;
import com.atguigu.lease.model.entity.*;
import com.atguigu.lease.model.enums.ItemType;
import com.atguigu.lease.web.admin.mapper.*;
import com.atguigu.lease.web.admin.service.*;
import com.atguigu.lease.web.admin.vo.attr.AttrValueVo;
import com.atguigu.lease.web.admin.vo.graph.GraphVo;
import com.atguigu.lease.web.admin.vo.room.RoomDetailVo;
import com.atguigu.lease.web.admin.vo.room.RoomItemVo;
import com.atguigu.lease.web.admin.vo.room.RoomQueryVo;
import com.atguigu.lease.web.admin.vo.room.RoomSubmitVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liubo
 * @description 针对表【room_info(房间信息表)】的数据库操作Service实现
 * @createDate 2023-07-24 15:48:00
 */
@Service
public class RoomInfoServiceImpl extends ServiceImpl<RoomInfoMapper, RoomInfo>
        implements RoomInfoService {

    @Autowired
    private GraphInfoService graphInfoService;

    @Autowired
    private RoomAttrValueService roomAttrValueService;

    @Autowired
    private RoomFacilityService roomFacilityService;

    @Autowired
    private RoomLabelService roomLabelService;

    @Autowired
    private RoomPaymentTypeService roomPaymentTypeService;

    @Autowired
    private RoomLeaseTermService roomLeaseTermService;

    @Override
    public void customSaveOrUpdate(RoomSubmitVo roomSubmitVo) {
        
        // 先判断是保存还是更新
        boolean isUpdate = roomSubmitVo.getId() != null;
        saveOrUpdate(roomSubmitVo);
        Long id = roomSubmitVo.getId();

        if (isUpdate) {
            // 先删除图片
            LambdaQueryWrapper<GraphInfo> graphInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
            graphInfoLambdaQueryWrapper.eq(GraphInfo::getItemId, id);
            graphInfoLambdaQueryWrapper.eq(GraphInfo::getItemType, ItemType.ROOM);
            graphInfoService.remove(graphInfoLambdaQueryWrapper);

            // roomAttValue
            LambdaQueryWrapper<RoomAttrValue> roomAttrValueLambdaQueryWrapper = new LambdaQueryWrapper<>();
            roomAttrValueLambdaQueryWrapper.eq(RoomAttrValue::getRoomId, id);
            roomAttrValueService.remove(roomAttrValueLambdaQueryWrapper);

            // roomFacilityService
            LambdaQueryWrapper<RoomFacility> roomFacilityLambdaQueryWrapper = new LambdaQueryWrapper<>();
            roomFacilityLambdaQueryWrapper.eq(RoomFacility::getRoomId, id);
            roomFacilityService.remove(roomFacilityLambdaQueryWrapper);

            // roomLabelService
            LambdaQueryWrapper<RoomLabel> roomLabelLambdaQueryWrapper = new LambdaQueryWrapper<>();
            roomLabelLambdaQueryWrapper.eq(RoomLabel::getRoomId, id);
            roomLabelService.remove(roomLabelLambdaQueryWrapper);

            // roomPaymentTypeService
            LambdaQueryWrapper<RoomPaymentType> roomPaymentTypeLambdaQueryWrapper = new LambdaQueryWrapper<>();
            roomPaymentTypeLambdaQueryWrapper.eq(RoomPaymentType::getRoomId, id);
            roomPaymentTypeService.remove(roomPaymentTypeLambdaQueryWrapper);

            // roomLeaseTermService
            LambdaQueryWrapper<RoomLeaseTerm> roomLeaseTermLambdaQueryWrapper = new LambdaQueryWrapper<>();
            roomLeaseTermLambdaQueryWrapper.eq(RoomLeaseTerm::getRoomId, id);
            roomLeaseTermService.remove(roomLeaseTermLambdaQueryWrapper);
        }

        // 保存中间表的数据
        List<GraphVo> graphVoList = roomSubmitVo.getGraphVoList();
        if(!CollectionUtils.isEmpty(graphVoList)) {
            ArrayList<GraphInfo> graphInfos = new ArrayList<>(graphVoList.size());
            for (GraphVo graphVo : graphVoList) {
                GraphInfo graphInfo = new GraphInfo();
                graphInfo.setId(id);
                graphInfo.setItemType(ItemType.ROOM);
                graphInfo.setName(graphVo.getName());
                graphInfo.setUrl(graphVo.getUrl());
                graphInfos.add(graphInfo);
            }
            graphInfoService.saveBatch(graphInfos);
        }

        // 2. 保存房间属性值中间表
        List<Long> attrValueIds = roomSubmitVo.getAttrValueIds();
        if (!CollectionUtils.isEmpty(attrValueIds)) {
            ArrayList<RoomAttrValue> roomAttrValueList = new ArrayList<>(attrValueIds.size());
            for (Long attrValueId : attrValueIds) {
                RoomAttrValue roomAttrValue = RoomAttrValue.builder()
                        .roomId(id)
                        .attrValueId(attrValueId)
                        .build();
                roomAttrValueList.add(roomAttrValue);
            }
            roomAttrValueService.saveBatch(roomAttrValueList);
        }

        // 3. 保存房间配套中间表
        List<Long> facilityInfoIds = roomSubmitVo.getFacilityInfoIds();
        if (!CollectionUtils.isEmpty(facilityInfoIds)) {
            ArrayList<RoomFacility> roomFacilityList = new ArrayList<>(facilityInfoIds.size());
            for (Long facilityId : facilityInfoIds) {
                RoomFacility roomFacility = RoomFacility.builder()
                        .roomId(id)
                        .facilityId(facilityId)
                        .build();
                roomFacilityList.add(roomFacility);
            }
            roomFacilityService.saveBatch(roomFacilityList);
        }

        // 4. 保存房间标签中间表
        List<Long> labelInfoIds = roomSubmitVo.getLabelInfoIds();
        if (!CollectionUtils.isEmpty(labelInfoIds)) {
            ArrayList<RoomLabel> roomLabelList = new ArrayList<>(labelInfoIds.size());
            for (Long labelId : labelInfoIds) {
                RoomLabel roomLabel = RoomLabel.builder()
                        .roomId(id)
                        .labelId(labelId)
                        .build();
                roomLabelList.add(roomLabel);
            }
            roomLabelService.saveBatch(roomLabelList);
        }

        // 5. 保存房间支付方式中间表
        List<Long> paymentTypeIds = roomSubmitVo.getPaymentTypeIds();
        if (!CollectionUtils.isEmpty(paymentTypeIds)) {
            ArrayList<RoomPaymentType> roomPaymentTypeList = new ArrayList<>(paymentTypeIds.size());
            for (Long paymentTypeId : paymentTypeIds) {
                RoomPaymentType roomPaymentType = RoomPaymentType.builder()
                        .roomId(id)
                        .paymentTypeId(paymentTypeId)
                        .build();
                roomPaymentTypeList.add(roomPaymentType);
            }
            roomPaymentTypeService.saveBatch(roomPaymentTypeList);
        }

        // 6. 保存房间租期中间表
        List<Long> leaseTermIds = roomSubmitVo.getLeaseTermIds();
        if (!CollectionUtils.isEmpty(leaseTermIds)) {
            ArrayList<RoomLeaseTerm> roomLeaseTermList = new ArrayList<>(leaseTermIds.size());
            for (Long leaseTermId : leaseTermIds) {
                RoomLeaseTerm roomLeaseTerm = RoomLeaseTerm.builder()
                        .roomId(id)
                        .leaseTermId(leaseTermId)
                        .build();
                roomLeaseTermList.add(roomLeaseTerm);
            }
            roomLeaseTermService.saveBatch(roomLeaseTermList);
        }
    }

    @Autowired
    private RoomInfoMapper roomInfoMapper;

    @Autowired
    private AttrValueMapper attrValueMapper;

    @Autowired
    private FacilityInfoMapper facilityInfoMapper;

    @Autowired
    private LabelInfoMapper labelInfoMapper;

    @Autowired
    private PaymentTypeMapper paymentTypeMapper;

    @Autowired
    private LeaseTermMapper leaseTermMapper;

    @Autowired
    private GraphInfoMapper graphInfoMapper;

    @Lazy
    @Autowired
    private ApartmentInfoService apartmentInfoService;

    @Override
    public void customPage(Page<RoomItemVo> page, RoomQueryVo queryVo) {
            roomInfoMapper.customQueryList(page, queryVo);
    }

    @Override
    public RoomDetailVo customGetById(Long id) {
        // 查询房间基本信息
        RoomInfo roomInfo = getById(id);
        if (roomInfo == null) {
            throw new LeaseException(ResultCodeEnum.DATA_ERROR);
        }

        // 查询所属公寓信息
        ApartmentInfo apartmentInfo = apartmentInfoService.getById(roomInfo.getApartmentId());

        // 查询图片列表
        List<GraphVo> graphVoList = graphInfoMapper.customQueryList(ItemType.ROOM, id);

        // 查询属性值列表
        List<AttrValueVo> attrValueVoList = attrValueMapper.customQueryList(id);

        // 查询配套设施列表
        List<FacilityInfo> facilityInfoList = facilityInfoMapper.customQueryListByRoomId(id);

        // 查询标签列表
        List<LabelInfo> labelInfoList = labelInfoMapper.customQueryListByRoomId(id);

        // 查询支付方式列表
        List<PaymentType> paymentTypeList = paymentTypeMapper.customQueryList(id);

        // 查询租期列表
        List<LeaseTerm> leaseTermList = leaseTermMapper.customQueryList(id);

        // 组装 VO
        RoomDetailVo roomDetailVo = new RoomDetailVo();
        roomDetailVo.setApartmentInfo(apartmentInfo);
        roomDetailVo.setGraphVoList(graphVoList);
        roomDetailVo.setAttrValueVoList(attrValueVoList);
        roomDetailVo.setFacilityInfoList(facilityInfoList);
        roomDetailVo.setLabelInfoList(labelInfoList);
        roomDetailVo.setPaymentTypeList(paymentTypeList);
        roomDetailVo.setLeaseTermList(leaseTermList);

        BeanUtils.copyProperties(roomInfo, roomDetailVo);
        return roomDetailVo;
    }

    @Override
    public void customRemoveById(Long id) {
        // 检查房间是否存在
        RoomInfo roomInfo = getById(id);
        if (roomInfo == null) {
            throw new LeaseException(ResultCodeEnum.DATA_ERROR);
        }

        // 删除中间表数据
        // 1. 删除图片
        LambdaQueryWrapper<GraphInfo> graphInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
        graphInfoLambdaQueryWrapper.eq(GraphInfo::getItemId, id);
        graphInfoLambdaQueryWrapper.eq(GraphInfo::getItemType, ItemType.ROOM);
        graphInfoService.remove(graphInfoLambdaQueryWrapper);

        // 2. 删除属性值中间表
        LambdaQueryWrapper<RoomAttrValue> roomAttrValueLambdaQueryWrapper = new LambdaQueryWrapper<>();
        roomAttrValueLambdaQueryWrapper.eq(RoomAttrValue::getRoomId, id);
        roomAttrValueService.remove(roomAttrValueLambdaQueryWrapper);

        // 3. 删除配套中间表
        LambdaQueryWrapper<RoomFacility> roomFacilityLambdaQueryWrapper = new LambdaQueryWrapper<>();
        roomFacilityLambdaQueryWrapper.eq(RoomFacility::getRoomId, id);
        roomFacilityService.remove(roomFacilityLambdaQueryWrapper);

        // 4. 删除标签中间表
        LambdaQueryWrapper<RoomLabel> roomLabelLambdaQueryWrapper = new LambdaQueryWrapper<>();
        roomLabelLambdaQueryWrapper.eq(RoomLabel::getRoomId, id);
        roomLabelService.remove(roomLabelLambdaQueryWrapper);

        // 5. 删除支付方式中间表
        LambdaQueryWrapper<RoomPaymentType> roomPaymentTypeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        roomPaymentTypeLambdaQueryWrapper.eq(RoomPaymentType::getRoomId, id);
        roomPaymentTypeService.remove(roomPaymentTypeLambdaQueryWrapper);

        // 6. 删除租期中间表
        LambdaQueryWrapper<RoomLeaseTerm> roomLeaseTermLambdaQueryWrapper = new LambdaQueryWrapper<>();
        roomLeaseTermLambdaQueryWrapper.eq(RoomLeaseTerm::getRoomId, id);
        roomLeaseTermService.remove(roomLeaseTermLambdaQueryWrapper);

        // 删除房间本身
        removeById(id);
    }
}




