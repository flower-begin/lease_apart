package com.atguigu.lease.web.app.service.impl;

import com.atguigu.lease.model.entity.ApartmentInfo;
import com.atguigu.lease.model.entity.FacilityInfo;
import com.atguigu.lease.model.entity.LabelInfo;
import com.atguigu.lease.model.enums.ItemType;
import com.atguigu.lease.web.app.mapper.*;
import com.atguigu.lease.web.app.service.ApartmentInfoService;
import com.atguigu.lease.web.app.vo.apartment.ApartmentDetailVo;
import com.atguigu.lease.web.app.vo.graph.GraphVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author liubo
 * @description 针对表【apartment_info(公寓信息表)】的数据库操作Service实现
 * @createDate 2023-07-26 11:12:39
 */
@Service
public class ApartmentInfoServiceImpl extends ServiceImpl<ApartmentInfoMapper, ApartmentInfo>
        implements ApartmentInfoService {

    @Autowired
    private GraphInfoMapper graphInfoMapper;

    @Autowired
    private FacilityInfoMapper facilityInfoMapper;

    @Autowired
    private LabelInfoMapper labelInfoMapper;

    @Autowired
    private RoomInfoMapper roomInfoMapper;

    @Override
    public ApartmentDetailVo getApartmentDetailById(Long id) {
        ApartmentInfo apartmentInfo = getById(id);
        if (apartmentInfo == null) {
            return null;
        }

        List<GraphVo> graphVoList = graphInfoMapper.customQueryList(ItemType.APARTMENT, id);
        List<FacilityInfo> facilityInfoList = facilityInfoMapper.customQueryList(id);
        List<LabelInfo> labelInfoList = labelInfoMapper.customQueryList(id);

        ApartmentDetailVo vo = new ApartmentDetailVo();
        BeanUtils.copyProperties(apartmentInfo, vo);
        vo.setGraphVoList(graphVoList);
        vo.setFacilityInfoList(facilityInfoList);
        vo.setLabelInfoList(labelInfoList);

        return vo;
    }

}




