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
     * 获取支付宝支付页面 HTML（浏览器直接打开此 URL 即可跳转支付宝收银台）
     */
    @GetMapping(value = "/pay-page/{orderNo}", produces = "text/html;charset=UTF-8")
    public String payPage(@PathVariable String orderNo) {
        PaymentRecord record = paymentService.lambdaQuery()
                .eq(PaymentRecord::getOrderNo, orderNo).one();
        log.info("【支付调试】pay-page 查询 orderNo={}, record={}", orderNo, record != null ? "存在" : "不存在");
        if (record == null) {
            return "<html><body><h2>订单不存在</h2></body></html>";
        }
        String html = record.getReceiptUrl();
        log.info("【支付调试】pay-page receiptUrl: isNull={}, isEmpty={}, length={}",
                html == null, html != null && html.isEmpty(), html != null ? html.length() : 0);
        if (html == null || html.isEmpty()) {
            return "<html><body><h2>支付页面已过期，请重新发起支付</h2></body></html>";
        }
        return html;
    }

    /**
     * 支付宝同步回调 (GET) — 处理完成重定向回前端
     */
    @GetMapping("/callback")
    public void callback(@RequestParam Map<String, String> params,
                         jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        log.info("支付宝同步回调: {}", params);
        try {
            boolean verified = alipayUtil.verifyCallback(params);
            if (!verified) {
                response.sendRedirect("http://localhost:3000/my-bills?payResult=fail");
                return;
            }
            String orderNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            paymentService.handlePaymentCallback(orderNo, tradeNo);
            response.sendRedirect("http://localhost:3000/my-bills?payResult=success");
        } catch (BusinessException e) {
            response.sendRedirect("http://localhost:3000/my-bills?payResult=fail");
        } catch (Exception e) {
            log.error("支付回调异常", e);
            response.sendRedirect("http://localhost:3000/my-bills?payResult=error");
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
