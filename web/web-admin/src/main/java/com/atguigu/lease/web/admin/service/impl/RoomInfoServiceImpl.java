package com.atguigu.lease.web.admin.service.impl;

import com.atguigu.lease.model.entity.*;
import com.atguigu.lease.model.enums.ItemType;
import com.atguigu.lease.web.admin.mapper.RoomInfoMapper;
import com.atguigu.lease.web.admin.service.*;
import com.atguigu.lease.web.admin.vo.graph.GraphVo;
import com.atguigu.lease.web.admin.vo.room.RoomSubmitVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
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
}




