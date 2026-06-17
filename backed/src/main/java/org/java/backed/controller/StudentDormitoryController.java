package org.java.backed.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.common.PageResult;
import org.java.backed.common.Result;
import org.java.backed.entity.Dormitory;
import org.java.backed.entity.StudentDormitory;
import org.java.backed.entity.SysUser;
import org.java.backed.mapper.SysUserMapper;
import org.java.backed.service.DormitoryService;
import org.java.backed.service.MinioService;
import org.java.backed.service.StudentDormitoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/students")
public class StudentDormitoryController {

    @Autowired
    private StudentDormitoryService studentService;

    @Autowired
    private MinioService minioService;

    @Autowired
    private DormitoryService dormitoryService;

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 分页+多条件查询
     */
    @GetMapping
    public Result<PageResult<StudentDormitory>> queryPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) String studentNo,
            @RequestParam(required = false) String dormitoryNo,
            @RequestParam(required = false) String paymentStatus) {
        Page<StudentDormitory> result = studentService.queryPage(page, pageSize, studentName, studentNo, dormitoryNo, paymentStatus);
        return Result.ok(PageResult.from(result));
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public Result<StudentDormitory> getById(@PathVariable Long id) {
        StudentDormitory student = studentService.getById(id);
        return student != null ? Result.ok(student) : Result.notFound("学生不存在");
    }

    /**
     * 新增学生
     */
    @PostMapping
    public Result<StudentDormitory> add(@RequestBody StudentDormitory student) {
        if (student.getStudentName() == null || student.getStudentName().isEmpty()) {
            return Result.badRequest("学生姓名不能为空");
        }
        if (student.getStudentNo() == null || student.getStudentNo().isEmpty()) {
            return Result.badRequest("学号不能为空");
        }
        if (student.getDormitoryNo() == null || student.getDormitoryNo().isEmpty()) {
            return Result.badRequest("宿舍号不能为空");
        }
        if (!dormitoryExists(student.getDormitoryNo())) {
            return Result.badRequest("宿舍号不存在，请先创建该宿舍");
        }
        studentService.save(student);
        return Result.ok(student);
    }

    /**
     * Excel批量导入
     */
    @PostMapping("/batch")
    public Result<Integer> importBatch(@RequestParam("file") MultipartFile file) {
        try {
            int count = studentService.importBatch(file);
            return Result.ok("成功导入 " + count + " 条学生数据", count);
        } catch (IOException e) {
            log.error("Excel导入失败", e);
            return Result.fail("Excel解析失败: " + e.getMessage());
        }
    }

    /**
     * 更新学生信息
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody StudentDormitory student) {
        if (student.getStudentName() == null || student.getStudentName().isEmpty()) {
            return Result.badRequest("学生姓名不能为空");
        }
        if (student.getStudentNo() == null || student.getStudentNo().isEmpty()) {
            return Result.badRequest("学号不能为空");
        }
        if (student.getDormitoryNo() == null || student.getDormitoryNo().isEmpty()) {
            return Result.badRequest("宿舍号不能为空");
        }
        if (!dormitoryExists(student.getDormitoryNo())) {
            return Result.badRequest("宿舍号不存在，请先创建该宿舍");
        }
        student.setId(id);
        studentService.updateById(student);
        return Result.ok();
    }

    /**
     * 删除学生
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        studentService.removeById(id);
        return Result.ok();
    }

    /**
     * 上传学生照片
     */
    @PostMapping("/{id}/photo")
    public Result<StudentDormitory> uploadPhoto(@PathVariable Long id,
                                                 @RequestParam("file") MultipartFile file) {
        StudentDormitory student = studentService.getById(id);
        if (student == null) return Result.notFound("学生不存在");

        String bucket = minioService.getFullBucketName("student-photos");
        String objectName = minioService.uploadFile(bucket, file);
        String url = "/api/files/student-photos/" + objectName;
        student.setPhoto(url);
        studentService.updateById(student);
        return Result.ok(student);
    }

    /**
     * 导出学生数据
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) String studentNo,
            @RequestParam(required = false) String dormitoryNo,
            @RequestParam(required = false) String paymentStatus) throws IOException {
        byte[] data = studentService.exportStudents(studentName, studentNo, dormitoryNo, paymentStatus);
        String filename = URLEncoder.encode("学生信息.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    /**
     * 人员管理 - 分页查询学生及关联账号信息
     */
    @GetMapping("/personnel")
    public Result<PageResult<Map<String, Object>>> queryPersonnel(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) String studentNo,
            @RequestParam(required = false) String dormitoryNo,
            @RequestParam(required = false) Boolean linked) {
        LambdaQueryWrapper<StudentDormitory> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(studentName != null, StudentDormitory::getStudentName, studentName);
        wrapper.eq(studentNo != null, StudentDormitory::getStudentNo, studentNo);
        wrapper.like(dormitoryNo != null, StudentDormitory::getDormitoryNo, dormitoryNo);
        if (linked != null) {
            if (linked) wrapper.isNotNull(StudentDormitory::getUserId);
            else wrapper.isNull(StudentDormitory::getUserId);
        }
        wrapper.orderByDesc(StudentDormitory::getCreateTime);
        Page<StudentDormitory> result = studentService.page(new Page<>(page, pageSize), wrapper);

        List<Map<String, Object>> enriched = result.getRecords().stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("studentName", s.getStudentName());
            m.put("studentNo", s.getStudentNo());
            m.put("dormitoryNo", s.getDormitoryNo());
            m.put("phone", s.getPhone());
            m.put("checkInDate", s.getCheckInDate());
            m.put("paymentStatus", s.getPaymentStatus());
            m.put("photo", s.getPhoto());
            m.put("userId", s.getUserId());
            m.put("createTime", s.getCreateTime());
            if (s.getUserId() != null) {
                SysUser u = sysUserMapper.selectById(s.getUserId());
                if (u != null) {
                    m.put("username", u.getUsername());
                    m.put("userStatus", u.getStatus());
                }
            }
            return m;
        }).collect(Collectors.toList());

        return Result.ok(PageResult.of(enriched, result.getTotal(), result.getCurrent(), result.getSize()));
    }

    /**
     * 关联学生记录到系统用户
     */
    @PutMapping("/{id}/link-user")
    public Result<Void> linkUser(@PathVariable Long id, @RequestBody Map<String, Long> params) {
        Long userId = params.get("userId");
        if (userId == null) return Result.badRequest("userId不能为空");

        StudentDormitory student = studentService.getById(id);
        if (student == null) return Result.notFound("学生记录不存在");

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) return Result.notFound("用户不存在");
        if (!"STUDENT".equals(user.getRole())) return Result.badRequest("只能关联STUDENT角色的用户");

        // 检查该用户是否已经关联到其他学生记录
        LambdaQueryWrapper<StudentDormitory> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(StudentDormitory::getUserId, userId);
        if (studentService.count(checkWrapper) > 0) return Result.conflict("该用户已关联到其他学生记录");

        student.setUserId(userId);
        studentService.updateById(student);
        return Result.ok();
    }

    /**
     * 取消学生记录与系统用户的关联
     */
    @PutMapping("/{id}/unlink-user")
    public Result<Void> unlinkUser(@PathVariable Long id) {
        StudentDormitory student = studentService.getById(id);
        if (student == null) return Result.notFound("学生记录不存在");
        student.setUserId(null);
        studentService.updateById(student);
        return Result.ok();
    }

    /**
     * 校验宿舍号是否存在
     */
    private boolean dormitoryExists(String dormitoryNo) {
        return dormitoryService.getActiveDormitories().stream()
                .anyMatch(d -> d.getDormitoryNo().equals(dormitoryNo));
    }
}
