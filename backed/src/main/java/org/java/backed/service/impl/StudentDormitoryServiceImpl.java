package org.java.backed.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.entity.StudentDormitory;
import org.java.backed.mapper.StudentDormitoryMapper;
import org.java.backed.service.StudentDormitoryService;
import org.java.backed.util.ExcelUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class StudentDormitoryServiceImpl extends ServiceImpl<StudentDormitoryMapper, StudentDormitory>
        implements StudentDormitoryService {

    @Override
    public Page<StudentDormitory> queryPage(int pageNum, int pageSize, String studentName,
                                             String studentNo, String dormitoryNo, String paymentStatus) {
        LambdaQueryWrapper<StudentDormitory> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(studentName != null && !studentName.isEmpty(), StudentDormitory::getStudentName, studentName);
        wrapper.eq(studentNo != null && !studentNo.isEmpty(), StudentDormitory::getStudentNo, studentNo);
        wrapper.like(dormitoryNo != null && !dormitoryNo.isEmpty(), StudentDormitory::getDormitoryNo, dormitoryNo);
        wrapper.eq(paymentStatus != null && !paymentStatus.isEmpty(), StudentDormitory::getPaymentStatus, paymentStatus);
        wrapper.orderByDesc(StudentDormitory::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importBatch(MultipartFile file) throws IOException {
        List<StudentDormitory> students = ExcelUtil.parseStudents(file.getInputStream());
        int count = 0;
        for (StudentDormitory student : students) {
            LambdaQueryWrapper<StudentDormitory> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(StudentDormitory::getStudentNo, student.getStudentNo());
            if (count(wrapper) > 0) {
                log.warn("学号已存在，跳过: {}", student.getStudentNo());
                continue;
            }
            save(student);
            count++;
        }
        return count;
    }

    @Override
    public byte[] exportStudents(String studentName, String studentNo, String dormitoryNo, String paymentStatus) throws IOException {
        LambdaQueryWrapper<StudentDormitory> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(studentName != null && !studentName.isEmpty(), StudentDormitory::getStudentName, studentName);
        wrapper.eq(studentNo != null && !studentNo.isEmpty(), StudentDormitory::getStudentNo, studentNo);
        wrapper.like(dormitoryNo != null && !dormitoryNo.isEmpty(), StudentDormitory::getDormitoryNo, dormitoryNo);
        wrapper.eq(paymentStatus != null && !paymentStatus.isEmpty(), StudentDormitory::getPaymentStatus, paymentStatus);
        wrapper.orderByDesc(StudentDormitory::getCreateTime);
        return ExcelUtil.exportStudents(list(wrapper));
    }
}
