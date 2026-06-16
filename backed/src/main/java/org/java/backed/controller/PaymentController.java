package org.java.backed.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.common.BusinessException;
import org.java.backed.common.PageResult;
import org.java.backed.common.Result;
import org.java.backed.entity.PaymentRecord;
import org.java.backed.service.PaymentService;
import org.java.backed.util.AlipayUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AlipayUtil alipayUtil;

    /**
     * 创建支付订单
     */
    @PostMapping("/create-order")
    public Result<PaymentRecord> createOrder(@RequestBody Map<String, Long> params) {
        Long billId = params.get("billId");
        if (billId == null) {
            return Result.badRequest("账单ID不能为空");
        }
        PaymentRecord record = paymentService.createOrder(billId);
        return Result.ok(record);
    }

    /**
     * 支付宝同步回调 (GET)
     */
    @GetMapping("/callback")
    public Result<String> callback(@RequestParam Map<String, String> params) {
        log.info("支付宝同步回调: {}", params);
        try {
            boolean verified = alipayUtil.verifyCallback(params);
            if (!verified) {
                return Result.fail("签名验证失败");
            }
            String orderNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            paymentService.handlePaymentCallback(orderNo, tradeNo);
            return Result.ok("支付成功");
        } catch (BusinessException e) {
            return Result.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("支付回调异常", e);
            return Result.fail("支付处理异常");
        }
    }

    /**
     * 支付宝异步通知 (POST)
     */
    @PostMapping("/notify")
    public String notify(@RequestParam Map<String, String> params) {
        log.info("支付宝异步通知: {}", params);
        try {
            boolean verified = alipayUtil.verifyNotify(params);
            if (!verified) {
                return "fail";
            }
            String tradeStatus = params.get("trade_status");
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                String orderNo = params.get("out_trade_no");
                String tradeNo = params.get("trade_no");
                paymentService.handlePaymentCallback(orderNo, tradeNo);
            }
            return "success";
        } catch (Exception e) {
            log.error("支付异步通知异常", e);
            return "fail";
        }
    }

    /**
     * 查询支付记录
     */
    @GetMapping("/records")
    public Result<PageResult<PaymentRecord>> queryRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String studentNo) {
        Page<PaymentRecord> result = paymentService.queryRecords(page, pageSize, studentNo);
        return Result.ok(PageResult.from(result));
    }

    /**
     * 查询支付详情
     */
    @GetMapping("/records/{orderNo}")
    public Result<PaymentRecord> getByOrderNo(@PathVariable String orderNo) {
        PaymentRecord record = paymentService.lambdaQuery()
                .eq(PaymentRecord::getOrderNo, orderNo).one();
        return record != null ? Result.ok(record) : Result.notFound("支付记录不存在");
    }
}
