package org.java.backed.service;

import java.util.List;
import java.util.Map;

/**
 * 统计报表服务接口
 */
public interface StatisticsService {

    Map<String, Object> getOverview();

    /** 获取单个学生的缴费概览 */
    Map<String, Object> getStudentOverview(Long studentId);

    List<Map<String, Object>> getCollectionRate(String semester);

    Map<String, Object> getArrears(int page, int pageSize, String dormitoryNo);

    Map<String, Object> getSemesterReport(String semester);
}
