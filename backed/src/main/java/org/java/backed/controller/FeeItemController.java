package org.java.backed.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.java.backed.common.PageResult;
import org.java.backed.common.Result;
import org.java.backed.entity.FeeItem;
import org.java.backed.service.FeeItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fee-items")
public class FeeItemController {

    @Autowired
    private FeeItemService feeItemService;

    /**
     * 分页查询
     */
    @GetMapping
    public Result<PageResult<FeeItem>> queryPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String feeType,
            @RequestParam(required = false) String status) {
        Page<FeeItem> result = feeItemService.queryPage(page, pageSize, feeType, status);
        return Result.ok(PageResult.from(result));
    }

    /**
     * 获取所有启用的收费项目
     */
    @GetMapping("/active")
    public Result<List<FeeItem>> getActive() {
        return Result.ok(feeItemService.getActiveFeeItems());
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public Result<FeeItem> getById(@PathVariable Long id) {
        FeeItem item = feeItemService.getById(id);
        return item != null ? Result.ok(item) : Result.notFound("收费项目不存在");
    }

    /**
     * 新增收费项目
     */
    @PostMapping
    public Result<FeeItem> add(@RequestBody FeeItem feeItem) {
        if (feeItem.getItemName() == null || feeItem.getItemName().isEmpty()) {
            return Result.badRequest("收费项目名称不能为空");
        }
        feeItemService.addFeeItem(feeItem);
        return Result.ok(feeItem);
    }

    /**
     * 更新收费项目（分布式锁）
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody FeeItem feeItem) {
        feeItem.setId(id);
        feeItemService.updateFeeItem(feeItem);
        return Result.ok();
    }

    /**
     * 删除收费项目
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        feeItemService.deleteFeeItem(id);
        return Result.ok();
    }
}
