package com.atguigu.lease.web.admin.custom.converter;

import com.atguigu.lease.model.enums.ItemType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * 将String -> ItemType
 * Converter<S,T>中的ST分别表示：原类型和目标类型，并且原类型必须是String
 */
@Component
public class StringToItemTypeConverter implements Converter<String, ItemType> {
    /**
     * 做类型转化的：参数用原类型
     * 返回用目标类型
     * @param source
     * @return
     */
    @Override
    public ItemType convert(String source) {
        // 先获取枚举类型的值
        ItemType[] values = ItemType.values();
        // 遍历里面的值
        for (ItemType itemType : values) {
            if (source.equals(itemType.getCode() + "")) {
                return itemType;
            }
        }
        throw new RuntimeException("传入参数错误，必须为1或2");
    }
}
