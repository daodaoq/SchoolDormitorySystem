package org.java.backed.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.common.PageResult;
import org.java.backed.common.Result;
import org.java.backed.entity.Dormitory;
import org.java.backed.entity.StudentDormitory;
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
     * 校验宿舍号是否存在
     */
    private boolean dormitoryExists(String dormitoryNo) {
        return dormitoryService.getActiveDormitories().stream()
                .anyMatch(d -> d.getDormitoryNo().equals(dormitoryNo));
    }
}
