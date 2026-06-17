package org.java.backed.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.common.BusinessException;
import org.java.backed.common.RedisLock;
import org.java.backed.config.CacheConfig;
import org.java.backed.entity.FeeItem;
import org.java.backed.mapper.FeeItemMapper;
import org.java.backed.service.FeeItemService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class FeeItemServiceImpl extends ServiceImpl<FeeItemMapper, FeeItem> implements FeeItemService {

    private final StringRedisTemplate stringRedisTemplate;

    public FeeItemServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Page<FeeItem> queryPage(int pageNum, int pageSize, String feeType, String status) {
        LambdaQueryWrapper<FeeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(feeType != null && !feeType.isEmpty(), FeeItem::getFeeType, feeType);
        wrapper.eq(status != null && !status.isEmpty(), FeeItem::getStatus, status);
        wrapper.orderByDesc(FeeItem::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.CACHE_FEE_ITEMS, key = "'active'")
    public List<FeeItem> getActiveFeeItems() {
        LambdaQueryWrapper<FeeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeeItem::getStatus, "ACTIVE");
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = CacheConfig.CACHE_FEE_ITEMS, allEntries = true)
    public boolean addFeeItem(FeeItem feeItem) {
        return save(feeItem);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = CacheConfig.CACHE_FEE_ITEMS, allEntries = true)
    public boolean updateFeeItem(FeeItem feeItem) {
        RedisLock lock = new RedisLock(stringRedisTemplate, "fee:update:" + feeItem.getId());
        try {
            if (!lock.lock(5000)) {
                throw new BusinessException(409, "收费项目正在被修改，请稍后重试");
            }
            return updateById(feeItem);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @CacheEvict(cacheNames = CacheConfig.CACHE_FEE_ITEMS, allEntries = true)
    public boolean deleteFeeItem(Long id) {
        return removeById(id);
    }
}
