package org.java.backed.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.java.backed.common.Result;
import org.java.backed.entity.StudentDormitory;
import org.java.backed.entity.SysMenu;
import org.java.backed.entity.SysRole;
import org.java.backed.entity.SysUser;
import org.java.backed.mapper.StudentDormitoryMapper;
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
    private final StudentDormitoryMapper studentDormitoryMapper;

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

        Map<String, Object> resultMap = new java.util.LinkedHashMap<>();
        resultMap.put("token", token);
        resultMap.put("username", user.getUsername());
        resultMap.put("realName", user.getRealName());
        resultMap.put("role", user.getRole());
        resultMap.put("roleName", role != null ? role.getRoleName() : "");
        resultMap.put("permissions", permissions);
        resultMap.put("menus", menuTree);

        // 如果是学生，附带其宿舍信息
        if ("STUDENT".equals(user.getRole())) {
            StudentDormitory studentRecord = studentDormitoryMapper.selectOne(
                    new LambdaQueryWrapper<StudentDormitory>().eq(StudentDormitory::getUserId, user.getId()));
            if (studentRecord != null) {
                resultMap.put("studentInfo", Map.of(
                        "id", studentRecord.getId(),
                        "studentNo", studentRecord.getStudentNo(),
                        "studentName", studentRecord.getStudentName(),
                        "dormitoryNo", studentRecord.getDormitoryNo(),
                        "phone", studentRecord.getPhone() != null ? studentRecord.getPhone() : "",
                        "checkInDate", studentRecord.getCheckInDate() != null ? studentRecord.getCheckInDate().toString() : "",
                        "paymentStatus", studentRecord.getPaymentStatus()
                ));
            }
        }

        return Result.ok(resultMap);
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
