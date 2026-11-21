package com.atguigu.lease.common.mybatisplus;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 自动填充 create_time / update_time。
 * <p>
 * ⚠️ 关键点（踩过的坑）：
 * {@link com.atguigu.lease.model.entity.BaseEntity} 里
 * <pre>
 *   create_time: fill = FieldFill.INSERT
 *   update_time: fill = FieldFill.INSERT_UPDATE
 * </pre>
 * MyBatis-Plus 对「带 fill 的字段」在 INSERT 时是**无条件写进 SQL** 的（为了让 handler 有机会赋值），
 * 而不是像普通字段那样按 NOT_NULL 策略跳过。
 * <p>
 * 所以如果 {@link #insertFill} 只填了 createTime、没填 updateTime，
 * 拼出来的 SQL 就是 {@code INSERT ... (create_time, update_time) VALUES (?, null)}，
 * 数据库里 update_time 是 NOT NULL，直接报：
 * <pre>
 *   SQLIntegrityConstraintViolationException: Column 'update_time' cannot be null
 * </pre>
 * 这就是"新增预约 / 新增公寓 / 新增房间"全部 500 的原因。
 * <p>
 * 因此：**INSERT 和 UPDATE 都必须把 updateTime 填上**。
 */
@Component
public class MybatisPlusObjectHandler implements MetaObjectHandler {

    /**
     * metaObject:获取类的信息
     * createTime/updateTime：为哪个属性赋值
     * Date.class：赋值的类型
     * new Date():具体赋的值
     *
     * @param metaObject 元对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        Date now = new Date();
        // 插入时两个时间都填，缺一不可（见类注释）
        this.strictInsertFill(metaObject, "createTime", Date.class, now);
        this.strictInsertFill(metaObject, "updateTime", Date.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", Date.class, new Date());
    }
}
