package org.java.backed.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.java.backed.common.PageResult;
import org.java.backed.common.Result;
import org.java.backed.entity.SysUser;
import org.java.backed.mapper.SysUserMapper;
import org.java.backed.service.MinioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final MinioService minioService;

    @GetMapping
    public Result<PageResult<SysUser>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(username != null, SysUser::getUsername, username);
        wrapper.eq(role != null, SysUser::getRole, role);
        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = userMapper.selectPage(new Page<>(page, pageSize), wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return Result.ok(PageResult.from(result));
    }

    @GetMapping("/{id}")
    public Result<SysUser> getById(@PathVariable Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) return Result.notFound("用户不存在");
        user.setPassword(null);
        return Result.ok(user);
    }

    @PostMapping
    public Result<SysUser> add(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        String realName = params.get("realName");
        String role = params.getOrDefault("role", "USER");

        if (username == null || username.isEmpty()) return Result.badRequest("用户名不能为空");
        if (password == null || password.isEmpty()) return Result.badRequest("密码不能为空");

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        if (userMapper.selectCount(wrapper) > 0) return Result.conflict("用户名已存在");

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setRole(role);
        user.setStatus("ACTIVE");
        userMapper.insert(user);
        user.setPassword(null);
        return Result.ok(user);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, String> params) {
        SysUser user = userMapper.selectById(id);
        if (user == null) return Result.notFound("用户不存在");

        if (params.containsKey("realName")) user.setRealName(params.get("realName"));
        if (params.containsKey("role")) user.setRole(params.get("role"));
        if (params.containsKey("status")) user.setStatus(params.get("status"));
        // 修改密码
        if (params.containsKey("password") && !params.get("password").isEmpty()) {
            user.setPassword(passwordEncoder.encode(params.get("password")));
        }
        userMapper.updateById(user);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) return Result.notFound("用户不存在");
        if ("admin".equals(user.getUsername())) return Result.fail("不能删除超级管理员");
        userMapper.deleteById(id);
        return Result.ok();
    }

    /**
     * 上传用户头像
     */
    @PostMapping("/{id}/avatar")
    public Result<SysUser> uploadAvatar(@PathVariable Long id,
                                         @RequestParam("file") MultipartFile file) {
        SysUser user = userMapper.selectById(id);
        if (user == null) return Result.notFound("用户不存在");

        String bucket = minioService.getFullBucketName("user-avatars");
        String objectName = minioService.uploadFile(bucket, file);
        String url = "/api/files/user-avatars/" + objectName;
        user.setAvatar(url);
        userMapper.updateById(user);
        user.setPassword(null);
        return Result.ok(user);
    }

    @PutMapping("/{id}/reset-password")
    public Result<String> resetPassword(@PathVariable Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) return Result.notFound("用户不存在");
        user.setPassword(passwordEncoder.encode("123456"));
        userMapper.updateById(user);
        return Result.ok("密码已重置为 123456");
    }
}
