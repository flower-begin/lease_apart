package com.atguigu.lease.web.admin.custom.converter;

import com.atguigu.lease.model.enums.ReleaseStatus;
import org.springframework.core.convert.converter.Converter;

public class StringToReleaseStatusConverter implements Converter<String, ReleaseStatus> {
    @Override
    public ReleaseStatus convert(String source) {
        if(source.equals("1"))
            return ReleaseStatus.RELEASED;
        if(source.equals("0"))
            return ReleaseStatus.NOT_RELEASED;
        throw new RuntimeException("传入参数错误，必须为1或0");
    }
}
