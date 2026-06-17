package org.java.backed.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.java.backed.common.PageResult;
import org.java.backed.common.Result;
import org.java.backed.entity.Dormitory;
import org.java.backed.service.DormitoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dormitories")
@RequiredArgsConstructor
public class DormitoryController {

    private final DormitoryService dormitoryService;

    /**
     * 分页查询
     */
    @GetMapping
    public Result<PageResult<Dormitory>> queryPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String dormitoryNo,
            @RequestParam(required = false) String building) {
        Page<Dormitory> result = dormitoryService.queryPage(page, pageSize, dormitoryNo, building);
        return Result.ok(PageResult.from(result));
    }

    /**
     * 获取所有启用宿舍（供下拉选择）
     */
    @GetMapping("/active")
    public Result<List<Dormitory>> getActive() {
        return Result.ok(dormitoryService.getActiveDormitories());
    }

    /**
     * 新增宿舍
     */
    @PostMapping
    public Result<Dormitory> add(@RequestBody Dormitory dormitory) {
        if (dormitory.getDormitoryNo() == null || dormitory.getDormitoryNo().isEmpty()) {
            return Result.badRequest("宿舍编号不能为空");
        }
        dormitory.setStatus(dormitory.getStatus() != null ? dormitory.getStatus() : "ACTIVE");
        dormitoryService.save(dormitory);
        return Result.ok(dormitory);
    }

    /**
     * 更新宿舍
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Dormitory dormitory) {
        if (dormitory.getDormitoryNo() == null || dormitory.getDormitoryNo().isEmpty()) {
            return Result.badRequest("宿舍编号不能为空");
        }
        dormitory.setId(id);
        dormitoryService.updateById(dormitory);
        return Result.ok();
    }

    /**
     * 删除宿舍
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dormitoryService.removeById(id);
        return Result.ok();
    }
}
