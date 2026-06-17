package org.java.backed.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.java.backed.common.Result;
import org.java.backed.entity.SysMenu;
import org.java.backed.entity.SysRole;
import org.java.backed.entity.SysUser;
import org.java.backed.mapper.SysRoleMapper;
import org.java.backed.mapper.SysUserMapper;
import org.java.backed.service.MenuService;
import org.java.backed.util.JwtUtil;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final MenuService menuService;

    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");

        if (username == null || password == null) {
            return Result.badRequest("用户名和密码不能为空");
        }

        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return Result.fail(401, "用户名或密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            return Result.fail(403, "账号已被禁用");
        }

        // 查找角色 → 获取权限+菜单
        SysRole role = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, user.getRole()));
        Long roleId = role != null ? role.getId() : null;
        List<String> permissions = roleId != null ? menuService.getRolePermissions(roleId) : List.of();
        List<SysMenu> menuTree = roleId != null ? menuService.getRoleMenuTree(roleId) : List.of();

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole(), permissions);

        return Result.ok(Map.of(
                "token", token,
                "username", user.getUsername(),
                "realName", user.getRealName(),
                "role", user.getRole(),
                "roleName", role != null ? role.getRoleName() : "",
                "permissions", permissions,
                "menus", menuTree
        ));
    }

    @GetMapping("/me")
    public Result<?> currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) return Result.notFound("用户不存在");
        user.setPassword(null);
        return Result.ok(user);
    }
}
