package org.java.backed.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.entity.StudentDormitory;
import org.java.backed.entity.SysUser;
import org.java.backed.mapper.StudentDormitoryMapper;
import org.java.backed.mapper.SysUserMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper userMapper;
    private final StudentDormitoryMapper studentMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createIfAbsent("admin", "admin123", "系统管理员", "ADMIN");
        createIfAbsent("teacher", "teacher123", "王老师", "TEACHER");

        // 为所有未关联用户的学生自动创建系统账号
        List<StudentDormitory> students = studentMapper.selectList(null);
        for (StudentDormitory s : students) {
            if (s.getUserId() == null) {
                SysUser user = createIfAbsent(s.getStudentNo(), "123456", s.getStudentName(), "STUDENT");
                if (user != null) {
                    s.setUserId(user.getId());
                    studentMapper.updateById(s);
                }
            }
        }
        log.info("默认账号初始化完成，已同步学生用户");
    }

    private SysUser createIfAbsent(String username, String password, String realName, String role) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser existing = userMapper.selectOne(wrapper);
        if (existing != null) return existing;
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setRole(role);
        user.setStatus("ACTIVE");
        userMapper.insert(user);
        log.info("账号已创建: {} / {} [{}]", username, password, role);
        return user;
    }
}
