package org.java.backed.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.java.backed.entity.StudentDormitory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 学生宿舍信息服务接口
 */
public interface StudentDormitoryService extends IService<StudentDormitory> {

    Page<StudentDormitory> queryPage(int pageNum, int pageSize, String studentName,
                                     String studentNo, String dormitoryNo, String paymentStatus);

    int importBatch(MultipartFile file) throws IOException;

    byte[] exportStudents(String studentName, String studentNo, String dormitoryNo, String paymentStatus) throws IOException;
}
