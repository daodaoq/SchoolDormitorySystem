package org.java.backed.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.java.backed.common.Result;
import org.java.backed.entity.SysMenu;
import org.java.backed.entity.SysRole;
import org.java.backed.mapper.SysRoleMapper;
import org.java.backed.service.MenuService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final SysRoleMapper roleMapper;
    private final MenuService menuService;

    @GetMapping
    public Result<List<SysRole>> list() {
        return Result.ok(roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId)));
    }

    @GetMapping("/{id}/menus")
    public Result<?> getRoleMenus(@PathVariable Long id) {
        return Result.ok(Map.of(
                "menus", menuService.getAllMenuTree(),
                "checkedKeys", menuService.getRoleMenuTree(id).stream()
                        .flatMap(m -> flattenIds(m).stream()).toList()
        ));
    }

    @PostMapping("/{id}/assign")
    public Result<Void> assign(@PathVariable Long id, @RequestBody Map<String, List<Long>> params) {
        menuService.assignPermissions(id, params.get("menuIds"));
        return Result.ok();
    }

    @PostMapping
    public Result<SysRole> add(@RequestBody SysRole role) {
        if (role.getRoleCode() == null || role.getRoleCode().isEmpty()) {
            return Result.badRequest("角色编码不能为空");
        }
        if (role.getRoleName() == null || role.getRoleName().isEmpty()) {
            return Result.badRequest("角色名称不能为空");
        }
        roleMapper.insert(role);
        return Result.ok(role);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysRole role) {
        if (role.getRoleCode() == null || role.getRoleCode().isEmpty()) {
            return Result.badRequest("角色编码不能为空");
        }
        if (role.getRoleName() == null || role.getRoleName().isEmpty()) {
            return Result.badRequest("角色名称不能为空");
        }
        role.setId(id);
        roleMapper.updateById(role);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleMapper.deleteById(id);
        return Result.ok();
    }

    private List<Long> flattenIds(SysMenu menu) {
        List<Long> ids = new java.util.ArrayList<>();
        ids.add(menu.getId());
        if (menu.getChildren() != null) {
            for (SysMenu child : menu.getChildren()) ids.addAll(flattenIds(child));
        }
        return ids;
    }
}
