package org.java.backed.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.java.backed.entity.PaymentRecord;

/**
 * 支付服务接口
 */
public interface PaymentService extends IService<PaymentRecord> {

    PaymentRecord createOrder(Long billId);

    void handlePaymentCallback(String orderNo, String tradeNo);

    Page<PaymentRecord> queryRecords(int pageNum, int pageSize, String studentNo);

    void closeTimeoutOrders();
}
