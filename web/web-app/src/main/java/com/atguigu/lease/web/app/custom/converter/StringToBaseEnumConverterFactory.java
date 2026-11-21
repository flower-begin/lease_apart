package com.atguigu.lease.web.app.custom.converter;

import com.atguigu.lease.model.enums.BaseEnum;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.stereotype.Component;

/**
 * 让「查询参数里的数字」能转成实现了 {@link BaseEnum} 的枚举。
 * <p>
 * 前端拿到的枚举是 {@code @JsonValue} 的 code（例如 LeaseStatus 的 1~7），
 * 所以 {@code ?leaseStatus=2} 必须能转成 {@code LeaseStatus.SIGNED}；
 * Spring 自带的 String→Enum 只认枚举名，这里按 code 匹配。
 * 后台 web-admin 有一份等价的实现，C 端原来漏了。
 */
@Component
public class StringToBaseEnumConverterFactory implements ConverterFactory<String, BaseEnum> {

    @Override
    public <T extends BaseEnum> Converter<String, T> getConverter(Class<T> targetType) {
        return source -> {
            String value = source == null ? "" : source.trim();
            if (value.isEmpty()) {
                return null;
            }
            for (T enumConstant : targetType.getEnumConstants()) {
                if (String.valueOf(enumConstant.getCode()).equals(value)) {
                    return enumConstant;
                }
                // 同时兼容直接传枚举名的情况，例如 ?leaseStatus=SIGNED
                if (enumConstant instanceof Enum<?> e && e.name().equalsIgnoreCase(value)) {
                    return enumConstant;
                }
            }
            throw new IllegalArgumentException("非法的枚举值: " + source);
        };
    }
}
