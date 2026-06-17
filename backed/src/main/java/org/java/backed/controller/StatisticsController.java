package org.java.backed.controller;

import org.java.backed.common.Result;
import org.java.backed.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    /**
     * 首页仪表盘概览
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(statisticsService.getOverview());
    }

    /**
     * 学生个人缴费概览
     */
    @GetMapping("/student/{studentId}")
    public Result<Map<String, Object>> studentOverview(@PathVariable Long studentId) {
        return Result.ok(statisticsService.getStudentOverview(studentId));
    }

    /**
     * 收缴率统计(按收费类型)
     */
    @GetMapping("/collection-rate")
    public Result<?> collectionRate(@RequestParam(required = false) String semester) {
        return Result.ok(statisticsService.getCollectionRate(semester));
    }

    /**
     * 欠费统计
     */
    @GetMapping("/arrears")
    public Result<Map<String, Object>> arrears(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String dormitoryNo) {
        return Result.ok(statisticsService.getArrears(page, pageSize, dormitoryNo));
    }

    /**
     * 学期报表
     */
    @GetMapping("/semester-report")
    public Result<Map<String, Object>> semesterReport(@RequestParam(required = false) String semester) {
        return Result.ok(statisticsService.getSemesterReport(semester));
    }

    /**
     * 月度报表
     */
    @GetMapping("/monthly-report")
    public Result<Map<String, Object>> monthlyReport(
            @RequestParam int year,
            @RequestParam int month) {
        // 简化实现：返回学期报表
        String semester = year + "-" + (month <= 8 ? "1" : "2");
        Map<String, Object> report = statisticsService.getSemesterReport(semester);
        report.put("year", year);
        report.put("month", month);
        return Result.ok(report);
    }
}
