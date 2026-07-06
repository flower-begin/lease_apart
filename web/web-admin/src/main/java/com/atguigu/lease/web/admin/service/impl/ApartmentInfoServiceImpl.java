package com.atguigu.lease.web.admin.service.impl;

import com.atguigu.lease.model.entity.*;
import com.atguigu.lease.model.enums.ItemType;
import com.atguigu.lease.web.admin.mapper.*;
import com.atguigu.lease.web.admin.service.*;
import com.atguigu.lease.web.admin.vo.apartment.ApartmentDetailVo;
import com.atguigu.lease.web.admin.vo.apartment.ApartmentItemVo;
import com.atguigu.lease.web.admin.vo.apartment.ApartmentQueryVo;
import com.atguigu.lease.web.admin.vo.apartment.ApartmentSubmitVo;
import com.atguigu.lease.web.admin.vo.fee.FeeValueVo;
import com.atguigu.lease.web.admin.vo.graph.GraphVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author liubo
 * @description 针对表【apartment_info(公寓信息表)】的数据库操作Service实现
 * @createDate 2023-07-24 15:48:00
 */
@Service
public class ApartmentInfoServiceImpl extends ServiceImpl<ApartmentInfoMapper, ApartmentInfo>
        implements ApartmentInfoService {
    // 调用mapper
    @Autowired
    private ApartmentFacilityService apartmentFacilityService;

    @Autowired
    private ApartmentFeeValueService apartmentFeeValueService;

    @Autowired
    private ApartmentLabelService apartmentLabelService;

    @Autowired
    private GraphInfoService graphInfoService;

    @Autowired
    private ApartmentInfoMapper apartmentInfoMapper;
    @Override
    public void customUpdateOrSave(ApartmentSubmitVo apartmentSubmitVo) {
        // 先了解本次是更新还是保存
        boolean flag = apartmentSubmitVo.getId() != null;  // 有id为true，无id为false

        saveOrUpdate(apartmentSubmitVo);
        Long apartmentId = apartmentSubmitVo.getId();

        // 说明id传进来了，就是更新操作
        if (flag) {
            // 更新
            // 先删除中间表(公寓配套中间表)
            LambdaQueryWrapper<ApartmentFacility> apartmentFacilityLambdaQueryWrapper = new LambdaQueryWrapper<>();
            apartmentFacilityLambdaQueryWrapper.eq(ApartmentFacility::getApartmentId, apartmentId);
            apartmentFacilityService.remove(apartmentFacilityLambdaQueryWrapper);

            // 先删除中间表(公寓杂费中间表)
            LambdaQueryWrapper<ApartmentFeeValue> apartmentFeeValueLambdaQueryWrapper = new LambdaQueryWrapper<>();
            apartmentFeeValueLambdaQueryWrapper.eq(ApartmentFeeValue::getApartmentId, apartmentId);
            apartmentFeeValueService.remove(apartmentFeeValueLambdaQueryWrapper);

            // 删除标签中间表
            LambdaQueryWrapper<ApartmentLabel> apartmentLabelLambdaQueryWrapper = new LambdaQueryWrapper<>();
            apartmentLabelLambdaQueryWrapper.eq(ApartmentLabel::getApartmentId, apartmentId);
            apartmentLabelService.remove(apartmentLabelLambdaQueryWrapper);

            // 删除图片
            LambdaQueryWrapper<GraphInfo> graphVoLambdaQueryWrapper = new LambdaQueryWrapper<>();
            graphVoLambdaQueryWrapper.eq(GraphInfo::getItemId, apartmentId);
            graphVoLambdaQueryWrapper.eq(GraphInfo::getItemType, ItemType.APARTMENT);
            graphInfoService.remove(graphVoLambdaQueryWrapper);
        }

        // ========== 重新插入关联数据 ==========

        // 1. 保存公寓配套中间表
        List<Long> facilityInfoIds = apartmentSubmitVo.getFacilityInfoIds();
        if (!CollectionUtils.isEmpty(facilityInfoIds)) {
            ArrayList<ApartmentFacility> apartmentFacilityList = new ArrayList<>(facilityInfoIds.size());
            for (Long facilityId : facilityInfoIds) {
                ApartmentFacility apartmentFacility = ApartmentFacility.builder()
                        .apartmentId(apartmentId)
                        .facilityId(facilityId)
                        .build();
                apartmentFacilityList.add(apartmentFacility);
            }
            apartmentFacilityService.saveBatch(apartmentFacilityList);
        }

        // 2. 保存公寓杂费中间表
        List<Long> feeValueIds = apartmentSubmitVo.getFeeValueIds();
        if (!CollectionUtils.isEmpty(feeValueIds)) {
            ArrayList<ApartmentFeeValue> apartmentFeeValueList = new ArrayList<>(feeValueIds.size());
            for (Long feeValueId : feeValueIds) {
                ApartmentFeeValue apartmentFeeValue = ApartmentFeeValue.builder()
                        .apartmentId(apartmentId)
                        .feeValueId(feeValueId)
                        .build();
                apartmentFeeValueList.add(apartmentFeeValue);
            }
            apartmentFeeValueService.saveBatch(apartmentFeeValueList);
        }

        // 3. 保存公寓标签中间表
        List<Long> labelIds = apartmentSubmitVo.getLabelIds();
        if (!CollectionUtils.isEmpty(labelIds)) {
            ArrayList<ApartmentLabel> apartmentLabelList = new ArrayList<>(labelIds.size());
            for (Long labelId : labelIds) {
                ApartmentLabel apartmentLabel = ApartmentLabel.builder()
                        .apartmentId(apartmentId)
                        .labelId(labelId)
                        .build();
                apartmentLabelList.add(apartmentLabel);
            }
            apartmentLabelService.saveBatch(apartmentLabelList);
        }

        // 4. 保存图片信息表
        List<GraphVo> graphVoList = apartmentSubmitVo.getGraphVoList();
        if (!CollectionUtils.isEmpty(graphVoList)) {
            ArrayList<GraphInfo> graphInfoList = new ArrayList<>(graphVoList.size());
            for (GraphVo graphVo : graphVoList) {
                GraphInfo graphInfo = new GraphInfo();
                graphInfo.setName(graphVo.getName());
                graphInfo.setUrl(graphVo.getUrl());
                graphInfo.setItemId(apartmentId);
                graphInfo.setItemType(ItemType.APARTMENT);
                graphInfoList.add(graphInfo);
            }
            graphInfoService.saveBatch(graphInfoList);
        }
    }

    @Override
    public void customPage(ApartmentQueryVo queryVo, IPage<ApartmentItemVo> page) {
        Page<ApartmentItemVo> pageResult = apartmentInfoMapper.customPageQuery(page, queryVo);
    }

    @Autowired
    private FacilityInfoMapper facilityInfoMapper;

    @Autowired
    private LabelInfoMapper labelInfoMapper;

    @Autowired
    private FeeValueMapper feeValueMapper;

    @Autowired
    private GraphInfoMapper graphInfoMapper;
    @Override
    public ApartmentDetailVo customGetById(Long id) {
        // 先根据前端传入的lease_agreement的id对应的公寓详情
        ApartmentInfo apartmentInfo = getById(id);
        // 再根据查询到的公寓id查询配套设施集合
        List<FacilityInfo> facilityInfos = facilityInfoMapper.customQueryList(id);
        // 再根据查询到的公寓id查询标签集合
        List<LabelInfo> labelInfos = labelInfoMapper.customQueryList(id);
        // 再根据查询到的公寓id查询杂费集合
        List<FeeValueVo> feeValues = feeValueMapper.customQueryList(id);
        // 再根据查询到的公寓id查询图片集合
        List<GraphVo> graphInfos = graphInfoMapper.customQueryList(ItemType.APARTMENT, id);

        // 将集合整合并赋值给vo
        ApartmentDetailVo apartmentDetailVo = new ApartmentDetailVo();
        apartmentDetailVo.setFacilityInfoList(facilityInfos);
        apartmentDetailVo.setLabelInfoList(labelInfos);
        apartmentDetailVo.setFeeValueVoList(feeValues);
        apartmentDetailVo.setGraphVoList(graphInfos);

        // 进行对象相同属性赋值
        // 参数1：提供属性值的对象(源对象)
        // 参数2：接收属性值的对象(目标对象)
        BeanUtils.copyProperties(apartmentInfo, apartmentDetailVo);
        return apartmentDetailVo;
    }
}




