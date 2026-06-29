package org.java.backed.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.java.backed.common.PageResult;
import org.java.backed.common.Result;
import org.java.backed.entity.OperationLog;
import org.java.backed.mapper.OperationLogMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 操作日志查询接口
 */
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final OperationLogMapper logMapper;

    /** 分页查询 */
    @GetMapping
    public Result<PageResult<OperationLog>> queryPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(username != null && !username.isEmpty(), OperationLog::getUsername, username);
        wrapper.eq(module != null && !module.isEmpty(), OperationLog::getModule, module);
        wrapper.eq(action != null && !action.isEmpty(), OperationLog::getAction, action);
        wrapper.eq(status != null && !status.isEmpty(), OperationLog::getStatus, status);
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(OperationLog::getCreateTime, LocalDate.parse(startDate).atStartOfDay());
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(OperationLog::getCreateTime, LocalDate.parse(endDate).plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(OperationLog::getCreateTime);
        Page<OperationLog> result = logMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return Result.ok(PageResult.from(result));
    }

    /** 模块列表（供筛选下拉） */
    @GetMapping("/modules")
    public Result<List<String>> modules() {
        List<String> modules = List.of("学生管理", "宿舍管理", "收费项目", "账单管理",
                "支付管理", "用户管理", "角色管理", "系统管理", "认证", "知识库");
        return Result.ok(modules);
    }

    /** 操作类型列表（供筛选下拉） */
    @GetMapping("/actions")
    public Result<List<String>> actions() {
        List<String> actions = List.of("新增", "修改", "删除", "登录", "支付", "导入", "导出", "上传");
        return Result.ok(actions);
    }

    /** 统计：按日期统计操作量 */
    @GetMapping("/stats")
    public Result<List<Map<String, Object>>> stats(
            @RequestParam(defaultValue = "7") int days) {
        // 简化实现：查询最近 N 天的操作计数
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(OperationLog::getCreateTime, LocalDate.now().minusDays(days).atStartOfDay());
        wrapper.groupBy(OperationLog::getCreateTime);
        // MyBatis-Plus groupBy 需要配合 select，这里返回总数即可
        Long total = logMapper.selectCount(wrapper);
        return Result.ok(List.of(Map.of("totalRecent", total, "days", days)));
    }
}
