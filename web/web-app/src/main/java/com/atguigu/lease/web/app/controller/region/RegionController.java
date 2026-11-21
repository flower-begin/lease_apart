package com.atguigu.lease.web.app.controller.region;


import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.model.entity.CityInfo;
import com.atguigu.lease.model.entity.DistrictInfo;
import com.atguigu.lease.model.entity.ProvinceInfo;
import com.atguigu.lease.web.app.service.CityInfoService;
import com.atguigu.lease.web.app.service.DistrictInfoService;
import com.atguigu.lease.web.app.service.ProvinceInfoService;
import com.atguigu.lease.web.app.vo.region.RegionNodeVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

@Tag(name = "地区信息")
@RestController
@RequestMapping("/app/region")
public class RegionController {

    @Autowired
    private ProvinceInfoService provinceInfoService;

    @Autowired
    private CityInfoService cityInfoService;

    @Autowired
    private DistrictInfoService districtInfoService;

    /**
     * 中文拼音排序器。
     * <p>
     * 为什么不在 SQL 里排序：数据库里省/市/区的 id 是「先有种子数据、后补全量」拼出来的，
     * 按 id 排会把后来导入的省份全都排到末尾，前端下拉框最上面永远是老的 4 个，
     * 看起来就像「导入没生效」。这里统一按中文拼音排序，行为稳定、跟 id 无关。
     */
    private static final Collator CN_COLLATOR = Collator.getInstance(Locale.CHINA);

    private static <T> List<T> sortedByName(List<T> list, Function<T, String> nameGetter) {
        if (list == null || list.isEmpty()) {
            return list;
        }
        List<T> copy = new ArrayList<>(list);
        copy.sort((x, y) -> {
            String a = nameGetter.apply(x);
            String b = nameGetter.apply(y);
            if (a == null) {
                return b == null ? 0 : 1;
            }
            if (b == null) {
                return -1;
            }
            return CN_COLLATOR.compare(a, b);
        });
        return copy;
    }

    @Operation(summary = "查询省份信息列表")
    @GetMapping("province/list")
    public Result<List<ProvinceInfo>> listProvince() {
        List<ProvinceInfo> list = provinceInfoService.list();
        return Result.ok(sortedByName(list, ProvinceInfo::getName));
    }

    @Operation(summary = "根据省份id查询城市信息列表")
    @GetMapping("city/listByProvinceId")
    public Result<List<CityInfo>> listCityInfoByProvinceId(@RequestParam Long id) {
        LambdaQueryWrapper<CityInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CityInfo::getProvinceId, id);
        List<CityInfo> list = cityInfoService.list(wrapper);
        return Result.ok(sortedByName(list, CityInfo::getName));
    }

    @GetMapping("district/listByCityId")
    @Operation(summary = "根据城市id查询区县信息")
    public Result<List<DistrictInfo>> listDistrictInfoByCityId(@RequestParam Long id) {
        LambdaQueryWrapper<DistrictInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DistrictInfo::getCityId, id);
        List<DistrictInfo> list = districtInfoService.list(wrapper);
        return Result.ok(sortedByName(list, DistrictInfo::getName));
    }

    /**
     * 一次返回「省 → 市 → 区」的完整树，官方 H5 的省市区选择器用这个接口。
     * 三级都按中文拼音排序。
     */
    @Operation(summary = "查询省市区树形结构")
    @GetMapping("listAsTree")
    public Result<List<RegionNodeVo>> listAsTree() {
        List<ProvinceInfo> provinces = sortedByName(provinceInfoService.list(), ProvinceInfo::getName);
        List<CityInfo> cities = sortedByName(cityInfoService.list(), CityInfo::getName);
        List<DistrictInfo> districts = sortedByName(districtInfoService.list(), DistrictInfo::getName);

        // 先按父级 id 分组，避免三层循环
        Map<Long, List<CityInfo>> citiesByProvince = new HashMap<>();
        for (CityInfo city : cities) {
            if (city.getProvinceId() == null) {
                continue;
            }
            citiesByProvince
                    .computeIfAbsent(city.getProvinceId().longValue(), k -> new ArrayList<>())
                    .add(city);
        }

        Map<Long, List<DistrictInfo>> districtsByCity = new HashMap<>();
        for (DistrictInfo district : districts) {
            if (district.getCityId() == null) {
                continue;
            }
            districtsByCity
                    .computeIfAbsent(district.getCityId().longValue(), k -> new ArrayList<>())
                    .add(district);
        }

        List<RegionNodeVo> tree = new ArrayList<>(provinces.size());
        for (ProvinceInfo province : provinces) {
            RegionNodeVo provinceNode = new RegionNodeVo();
            provinceNode.setId(province.getId());
            provinceNode.setName(province.getName());

            List<CityInfo> cityList = citiesByProvince.getOrDefault(province.getId(), List.of());
            List<RegionNodeVo> cityNodes = new ArrayList<>(cityList.size());
            for (CityInfo city : cityList) {
                RegionNodeVo cityNode = new RegionNodeVo();
                cityNode.setId(city.getId());
                cityNode.setName(city.getName());

                List<DistrictInfo> districtList = districtsByCity.getOrDefault(city.getId(), List.of());
                List<RegionNodeVo> districtNodes = new ArrayList<>(districtList.size());
                for (DistrictInfo district : districtList) {
                    RegionNodeVo districtNode = new RegionNodeVo();
                    districtNode.setId(district.getId());
                    districtNode.setName(district.getName());
                    districtNodes.add(districtNode);
                }
                cityNode.setChildren(districtNodes);

                cityNodes.add(cityNode);
            }
            provinceNode.setChildren(cityNodes);

            tree.add(provinceNode);
        }

        return Result.ok(tree);
    }
}
