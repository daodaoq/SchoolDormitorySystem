package org.java.backed.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.java.backed.common.Result;
import org.java.backed.common.annotation.OpLog;
import org.java.backed.entity.StudentDormitory;
import org.java.backed.entity.SysMenu;
import org.java.backed.entity.SysRole;
import org.java.backed.entity.SysUser;
import org.java.backed.mapper.StudentDormitoryMapper;
import org.java.backed.mapper.SysRoleMapper;
import org.java.backed.mapper.SysUserMapper;
import org.java.backed.service.MenuService;
import org.java.backed.util.JwtUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    private final StringRedisTemplate stringRedisTemplate;

    /** 登录限流：同一IP每分钟最多10次尝试。Redis不可用时自动跳过限流 */
    private boolean checkRateLimit(String key) {
        try {
            String redisKey = "rate_limit:login:" + key;
            Long count = stringRedisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1) {
                stringRedisTemplate.expire(redisKey, 1, TimeUnit.MINUTES);
            }
            return count != null && count <= 10;
        } catch (Exception e) {
            // Redis 不可用 → 跳过限流，允许登录
            return true;
        }
    }

    @OpLog(module = "认证", action = "登录", description = "用户登录", logParams = false)
    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> params,
                           @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
                           jakarta.servlet.http.HttpServletRequest request) {
        String username = params.get("username");
        String password = params.get("password");

        if (username == null || password == null) {
            return Result.badRequest("用户名和密码不能为空");
        }

        // 限流：按用户名 + IP
        String clientIp = forwardedFor != null ? forwardedFor.split(",")[0].trim() : request.getRemoteAddr();
        if (!checkRateLimit(username) || !checkRateLimit(clientIp)) {
            return Result.fail(429, "请求过于频繁，请稍后再试");
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
