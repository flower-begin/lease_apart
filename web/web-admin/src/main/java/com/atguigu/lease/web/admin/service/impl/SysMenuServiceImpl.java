package com.atguigu.lease.web.admin.service.impl;

import com.atguigu.lease.model.entity.SysMenu;
import com.atguigu.lease.web.admin.mapper.SysUserRoleMapper;
import com.atguigu.lease.web.admin.service.SysMenuService;
import com.atguigu.lease.web.admin.vo.system.menu.MenuVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.lease.web.admin.mapper.SysMenuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Override
    public List<MenuVo> tree() {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysMenu::getSort);
        return buildTree(this.list(wrapper));
    }

    @Override
    public List<MenuVo> treeByUserId(Long userId) {
        return buildTree(sysUserRoleMapper.selectMenusByUserId(userId));
    }

    @Override
    public List<MenuVo> buildTree(List<SysMenu> menus) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }
        // LinkedHashMap 保证父节点先于子节点出现的顺序（SQL 已按 sort 排好）
        Map<Long, MenuVo> voMap = new LinkedHashMap<>(menus.size());
        for (SysMenu menu : menus) {
            voMap.put(menu.getId(), toVo(menu));
        }

        List<MenuVo> roots = new ArrayList<>();
        for (SysMenu menu : menus) {
            MenuVo vo = voMap.get(menu.getId());
            Long parentId = menu.getParentId() == null ? 0L : menu.getParentId();
            MenuVo parent = voMap.get(parentId);
            // 父级没被授权时，子节点挂到根上，避免整棵子树消失
            if (parentId == 0L || parent == null) {
                roots.add(vo);
            } else {
                parent.getChildren().add(vo);
            }
        }
        return roots;
    }

    private MenuVo toVo(SysMenu menu) {
        MenuVo vo = new MenuVo();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setName(menu.getName());
        vo.setType(menu.getType());
        vo.setPath(menu.getPath());
        vo.setIcon(menu.getIcon());
        vo.setPerms(menu.getPerms());
        vo.setSort(menu.getSort());
        return vo;
    }
}
