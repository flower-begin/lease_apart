package com.atguigu.lease.web.admin.vo.system.role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "角色分配菜单请求体")
@Data
public class RoleMenuVo {

    @Schema(description = "角色id")
    private Long roleId;

    @Schema(description = "勾选的菜单id（含目录/菜单/按钮）")
    private List<Long> menuIds;
}
