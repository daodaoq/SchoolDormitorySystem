package org.java.backed.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.java.backed.common.PageResult;
import org.java.backed.common.Result;
import org.java.backed.entity.PaymentBill;
import org.java.backed.service.PaymentBillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bills")
public class PaymentBillController {

    @Autowired
    private PaymentBillService billService;

    /**
     * 分页查询账单
     */
    @GetMapping
    public Result<PageResult<PaymentBill>> queryPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String studentNo,
            @RequestParam(required = false) String dormitoryNo,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String feeType) {
        Page<PaymentBill> result = billService.queryPage(page, pageSize, studentNo, dormitoryNo, semester, status, feeType);
        return Result.ok(PageResult.from(result));
    }

    /**
     * 查询账单详情
     */
    @GetMapping("/{id}")
    public Result<PaymentBill> getById(@PathVariable Long id) {
        PaymentBill bill = billService.getById(id);
        return bill != null ? Result.ok(bill) : Result.notFound("账单不存在");
    }

    /**
     * 手动触发账单生成
     */
    @PostMapping("/generate")
    public Result<Integer> generateBills(@RequestBody Map<String, Object> params) {
        String semester = (String) params.get("semester");
        if (semester == null || semester.isEmpty()) {
            return Result.badRequest("学期不能为空");
        }
        @SuppressWarnings("unchecked")
        List<Long> feeItemIds = (List<Long>) params.get("feeItemIds");
        int count = billService.generateBills(semester, feeItemIds);
        return Result.ok("成功生成 " + count + " 条账单", count);
    }

    /**
     * 管理员修正账单状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String status = params.get("status");
        String remark = params.get("remark");
        billService.updateBillStatus(id, status, remark);
        return Result.ok();
    }

    /**
     * 导出账单
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String studentNo,
            @RequestParam(required = false) String dormitoryNo,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String status) throws IOException {
        byte[] data = billService.exportBills(studentNo, dormitoryNo, semester, status);
        String filename = URLEncoder.encode("缴费账单.xlsx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
