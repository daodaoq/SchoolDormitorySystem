package org.java.backed.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.java.backed.entity.Dormitory;

import java.util.List;

public interface DormitoryService extends IService<Dormitory> {

    Page<Dormitory> queryPage(int page, int pageSize, String dormitoryNo, String building);

    List<Dormitory> getActiveDormitories();
}
