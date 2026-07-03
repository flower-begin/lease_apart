package com.atguigu.lease.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;


public enum ItemType implements BaseEnum {

    APARTMENT(1, "公寓"),

    ROOM(2, "房间");


    @EnumValue  // 作用于持久层与数据库之间，把字符串转化成数据库中对应的数据类型
    @JsonValue  // 前端传递后端用实体类(枚举类)接受时告诉它，不要给整个赋值，赋给code就行，并且相应给前端的时候也不是响应整个类，而是将里面的code值转成json数据的一部分
    private Integer code;
    private String name;

    @Override
    public Integer getCode() {
        return this.code;
    }


    @Override
    public String getName() {
        return name;
    }

    ItemType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

}
