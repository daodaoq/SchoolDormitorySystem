package org.java.backed.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.entity.FeeItem;
import org.java.backed.entity.PaymentBill;
import org.java.backed.entity.StudentDormitory;
import org.java.backed.service.FeeItemService;
import org.java.backed.service.PaymentBillService;
import org.java.backed.service.PaymentService;
import org.java.backed.service.StatisticsService;
import org.java.backed.service.StudentDormitoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.java.backed.config.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StatisticsServiceImpl implements StatisticsService {

    /** fee_type 英文代码 → 中文名称 */
    private static final Map<String, String> FEE_TYPE_CN = Map.of(
            "ACCOMMODATION", "住宿费",
            "WATER", "水费",
            "ELECTRICITY", "电费",
            "AC", "空调费",
            "NETWORK", "网络费",
            "OTHER", "其他"
    );

    private String toFeeTypeCn(String feeType) {
        return FEE_TYPE_CN.getOrDefault(feeType, feeType);
    }

    @Autowired
    private StudentDormitoryService studentService;

    @Autowired
    private PaymentBillService billService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private FeeItemService feeItemService;

    @Override
    @Cacheable(cacheNames = CacheConfig.CACHE_STATS, key = "'overview'")
    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();

        long totalStudents = studentService.count();
        overview.put("totalStudents", totalStudents);

        String currentSemester = getCurrentSemester();
        LambdaQueryWrapper<PaymentBill> billWrapper = new LambdaQueryWrapper<>();
        billWrapper.eq(PaymentBill::getSemester, currentSemester);
        List<PaymentBill> semesterBills = billService.list(billWrapper);

        long totalBills = semesterBills.size();
        BigDecimal totalAmount = semesterBills.stream().map(PaymentBill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paidAmount = semesterBills.stream().map(PaymentBill::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        overview.put("totalBillsThisSemester", totalBills);
        overview.put("totalAmount", totalAmount);
        overview.put("paidAmount", paidAmount);

        BigDecimal collectionRate = totalAmount.compareTo(BigDecimal.ZERO) > 0
                ? paidAmount.multiply(BigDecimal.valueOf(100)).divide(totalAmount, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        overview.put("collectionRate", collectionRate);

        LambdaQueryWrapper<PaymentBill> overdueWrapper = new LambdaQueryWrapper<>();
        overdueWrapper.eq(PaymentBill::getStatus, "OVERDUE");
        long overdueCount = billService.count(overdueWrapper);
        BigDecimal overdueAmount = billService.list(overdueWrapper).stream()
                .map(b -> b.getAmount().subtract(b.getPaidAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        overview.put("overdueCount", overdueCount);
        overview.put("overdueAmount", overdueAmount);

        LambdaQueryWrapper<PaymentBill> unpaidWrapper = new LambdaQueryWrapper<>();
        unpaidWrapper.eq(PaymentBill::getStatus, "UNPAID");
        long unpaidCount = billService.count(unpaidWrapper);
        overview.put("unpaidCount", unpaidCount);

        // 构建 feeItemId → feeType 映射，解决 PaymentBill.feeType 为 transient 字段的问题
        Map<Long, String> feeTypeMap = feeItemService.list().stream()
                .collect(Collectors.toMap(
                        FeeItem::getId,
                        fi -> fi.getFeeType() != null ? fi.getFeeType() : "OTHER",
                        (a, b) -> a));

        List<Map<String, Object>> feeTypeDistribution = new ArrayList<>();
        Map<String, List<PaymentBill>> grouped = new HashMap<>();
        for (PaymentBill bill : semesterBills) {
            String feeType = feeTypeMap.getOrDefault(bill.getFeeItemId(), "OTHER");
            grouped.computeIfAbsent(feeType, k -> new ArrayList<>()).add(bill);
        }
        for (Map.Entry<String, List<PaymentBill>> entry : grouped.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("feeType", toFeeTypeCn(entry.getKey()));
            item.put("count", entry.getValue().size());
            item.put("totalAmount", entry.getValue().stream()
                    .map(PaymentBill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
            feeTypeDistribution.add(item);
        }
        overview.put("feeTypeDistribution", feeTypeDistribution);
        return overview;
    }

    @Override
    public Map<String, Object> getStudentOverview(Long studentId) {
        Map<String, Object> overview = new LinkedHashMap<>();

        LambdaQueryWrapper<PaymentBill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentBill::getStudentId, studentId);
        List<PaymentBill> bills = billService.list(wrapper);

        long totalBills = bills.size();
        BigDecimal totalAmount = bills.stream().map(PaymentBill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paidAmount = bills.stream().map(PaymentBill::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        overview.put("totalBills", totalBills);
        overview.put("totalAmount", totalAmount);
        overview.put("paidAmount", paidAmount);

        BigDecimal collectionRate = totalAmount.compareTo(BigDecimal.ZERO) > 0
                ? paidAmount.multiply(BigDecimal.valueOf(100)).divide(totalAmount, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        overview.put("collectionRate", collectionRate);

        long overdueCount = bills.stream().filter(b -> "OVERDUE".equals(b.getStatus())).count();
        BigDecimal overdueAmount = bills.stream()
                .filter(b -> "OVERDUE".equals(b.getStatus()))
                .map(b -> b.getAmount().subtract(b.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        overview.put("overdueCount", overdueCount);
        overview.put("overdueAmount", overdueAmount);

        long unpaidCount = bills.stream().filter(b -> "UNPAID".equals(b.getStatus())).count();
        overview.put("unpaidCount", unpaidCount);

        return overview;
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.CACHE_STATS, key = "'collection_' + (#semester ?: 'all')")
    public List<Map<String, Object>> getCollectionRate(String semester) {
        if (semester == null || semester.isEmpty()) semester = getCurrentSemester();
        List<Map<String, Object>> result = new ArrayList<>();

        LambdaQueryWrapper<PaymentBill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentBill::getSemester, semester);
        List<PaymentBill> bills = billService.list(wrapper);

        Map<Long, String> feeTypeMap = feeItemService.list().stream()
                .collect(Collectors.toMap(
                        FeeItem::getId,
                        fi -> fi.getFeeType() != null ? fi.getFeeType() : "OTHER",
                        (a, b) -> a));

        Map<String, List<PaymentBill>> grouped = new HashMap<>();
        for (PaymentBill bill : bills) {
            String feeType = feeTypeMap.getOrDefault(bill.getFeeItemId(), "OTHER");
            grouped.computeIfAbsent(feeType, k -> new ArrayList<>()).add(bill);
        }

        for (Map.Entry<String, List<PaymentBill>> entry : grouped.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            List<PaymentBill> typeBills = entry.getValue();
            BigDecimal totalAmount = typeBills.stream().map(PaymentBill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal paidAmount = typeBills.stream().map(PaymentBill::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal collected = totalAmount.compareTo(BigDecimal.ZERO) > 0
                    ? paidAmount.multiply(BigDecimal.valueOf(100)).divide(totalAmount, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            item.put("feeType", toFeeTypeCn(entry.getKey()));
            item.put("totalCount", typeBills.size());
            item.put("collectedCount", typeBills.stream().filter(b -> "PAID".equals(b.getStatus())).count());
            item.put("totalAmount", totalAmount);
            item.put("paidAmount", paidAmount);
            item.put("collectionRate", collected);
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Object> getArrears(int page, int pageSize, String dormitoryNo) {
        LambdaQueryWrapper<PaymentBill> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PaymentBill::getStatus, List.of("UNPAID", "OVERDUE"));
        List<PaymentBill> allArrears = billService.list(wrapper);

        List<Map<String, Object>> arrearsList = new ArrayList<>();
        for (PaymentBill bill : allArrears) {
            StudentDormitory student = studentService.getById(bill.getStudentId());
            if (student == null) continue;
            if (dormitoryNo != null && !dormitoryNo.isEmpty() && !student.getDormitoryNo().contains(dormitoryNo)) continue;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("billNo", bill.getBillNo());
            item.put("studentName", student.getStudentName());
            item.put("studentNo", student.getStudentNo());
            item.put("dormitoryNo", student.getDormitoryNo());
            item.put("amount", bill.getAmount());
            item.put("paidAmount", bill.getPaidAmount());
            item.put("arrears", bill.getAmount().subtract(bill.getPaidAmount()));
            item.put("dueDate", bill.getDueDate());
            item.put("status", bill.getStatus());
            item.put("semester", bill.getSemester());
            arrearsList.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        BigDecimal totalArrears = arrearsList.stream()
                .map(m -> (BigDecimal) m.get("arrears")).reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("totalArrears", totalArrears);
        result.put("totalCount", arrearsList.size());
        result.put("records", arrearsList);
        return result;
    }

    @Override
    public Map<String, Object> getSemesterReport(String semester) {
        if (semester == null || semester.isEmpty()) semester = getCurrentSemester();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("semester", semester);
        report.put("overview", getOverview());
        report.put("collectionRate", getCollectionRate(semester));
        report.put("arrears", getArrears(1, 9999, null));
        report.put("generateTime", LocalDate.now().toString());
        return report;
    }

    private String getCurrentSemester() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        return month <= 8 ? year + "-1" : year + "-2";
    }
}
