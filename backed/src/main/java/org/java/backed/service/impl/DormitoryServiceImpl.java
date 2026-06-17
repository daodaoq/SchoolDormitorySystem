package org.java.backed.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.java.backed.config.CacheConfig;
import org.java.backed.entity.Dormitory;
import org.java.backed.mapper.DormitoryMapper;
import org.java.backed.service.DormitoryService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Cacheable(cacheNames = CacheConfig.CACHE_DORMITORIES, key = "'active'")
    public List<Dormitory> getActiveDormitories() {
        LambdaQueryWrapper<Dormitory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dormitory::getStatus, "ACTIVE");
        wrapper.orderByAsc(Dormitory::getDormitoryNo);
        return list(wrapper);
    }

    @Override
    @CacheEvict(cacheNames = CacheConfig.CACHE_DORMITORIES, allEntries = true)
    public boolean save(Dormitory entity) { return super.save(entity); }

    @Override
    @CacheEvict(cacheNames = CacheConfig.CACHE_DORMITORIES, allEntries = true)
    public boolean updateById(Dormitory entity) { return super.updateById(entity); }

    @Override
    @CacheEvict(cacheNames = CacheConfig.CACHE_DORMITORIES, allEntries = true)
    public boolean removeById(java.io.Serializable id) { return super.removeById(id); }
}
