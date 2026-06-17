package org.java.backed.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.java.backed.entity.Dormitory;
import org.java.backed.mapper.DormitoryMapper;
import org.java.backed.service.DormitoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DormitoryServiceImpl extends ServiceImpl<DormitoryMapper, Dormitory> implements DormitoryService {

    @Override
    public Page<Dormitory> queryPage(int page, int pageSize, String dormitoryNo, String building) {
        LambdaQueryWrapper<Dormitory> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(dormitoryNo != null, Dormitory::getDormitoryNo, dormitoryNo);
        wrapper.like(building != null, Dormitory::getBuilding, building);
        wrapper.orderByAsc(Dormitory::getDormitoryNo);
        return page(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public List<Dormitory> getActiveDormitories() {
        LambdaQueryWrapper<Dormitory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dormitory::getStatus, "ACTIVE");
        wrapper.orderByAsc(Dormitory::getDormitoryNo);
        return list(wrapper);
    }
}
