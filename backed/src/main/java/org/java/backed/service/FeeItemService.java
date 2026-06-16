package org.java.backed.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.java.backed.entity.FeeItem;

import java.util.List;

/**
 * 收费项目服务接口
 */
public interface FeeItemService extends IService<FeeItem> {

    Page<FeeItem> queryPage(int pageNum, int pageSize, String feeType, String status);

    List<FeeItem> getActiveFeeItems();

    boolean addFeeItem(FeeItem feeItem);

    boolean updateFeeItem(FeeItem feeItem);

    boolean deleteFeeItem(Long id);
}
