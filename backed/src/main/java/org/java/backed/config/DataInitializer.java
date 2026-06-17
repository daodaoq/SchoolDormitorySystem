package org.java.backed.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.entity.SysUser;
import org.java.backed.mapper.SysUserMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createIfAbsent("admin", "admin123", "系统管理员", "ADMIN");
        createIfAbsent("teacher", "teacher123", "王老师", "TEACHER");
        createIfAbsent("student", "student123", "张同学", "STUDENT");
        log.info("默认账号初始化完成");
    }

    private void createIfAbsent(String username, String password, String realName, String role) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        if (userMapper.selectCount(wrapper) == 0) {
            SysUser user = new SysUser();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setRealName(realName);
            user.setRole(role);
            user.setStatus("ACTIVE");
            userMapper.insert(user);
            log.info("账号已创建: {} / {} [{}]", username, password, role);
        }
    }
}
