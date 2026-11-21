package com.atguigu.lease.web.admin.vo.system.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "给员工分配角色请求体")
@Data
public class UserRoleVo {

    @Schema(description = "员工id")
    private Long userId;

    @Schema(description = "角色id列表")
    private List<Long> roleIds;
}
