package org.java.backed.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.java.backed.entity.SysMenu;
import org.java.backed.entity.SysRoleMenu;
import org.java.backed.mapper.SysMenuMapper;
import org.java.backed.mapper.SysRoleMenuMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService extends ServiceImpl<SysMenuMapper, SysMenu> {

    private final SysRoleMenuMapper roleMenuMapper;

    /** 获取角色菜单(树形) */
    public List<SysMenu> getRoleMenuTree(Long roleId) {
        List<SysMenu> all = roleMenuMapper.findMenusByRoleId(roleId);
        return buildTree(all, 0L);
    }

    /** 获取全部菜单(树形) */
    public List<SysMenu> getAllMenuTree() {
        List<SysMenu> all = list(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSortOrder));
        return buildTree(all, 0L);
    }

    /** 分配权限 */
    @Transactional
    public void assignPermissions(Long roleId, List<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        if (menuIds != null && !menuIds.isEmpty()) {
            for (Long menuId : menuIds) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                roleMenuMapper.insert(rm);
            }
        }
    }

    /** 获取角色权限标识 */
    public List<String> getRolePermissions(Long roleId) {
        return roleMenuMapper.findPermissionsByRoleId(roleId);
    }

    private List<SysMenu> buildTree(List<SysMenu> list, Long parentId) {
        return list.stream()
                .filter(m -> m.getParentId().equals(parentId))
                .peek(m -> m.setChildren(buildTree(list, m.getId())))
                .collect(Collectors.toList());
    }
}
