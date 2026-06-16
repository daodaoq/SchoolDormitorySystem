package org.java.backed.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.java.backed.entity.PaymentBill;

import java.io.IOException;
import java.util.List;

/**
 * 缴费账单服务接口
 */
public interface PaymentBillService extends IService<PaymentBill> {

    Page<PaymentBill> queryPage(int pageNum, int pageSize, String studentNo,
                                String dormitoryNo, String semester, String status, String feeType);

    int generateBills(String semester, List<Long> feeItemIds);

    boolean updateBillStatus(Long billId, String status, String remark);

    byte[] exportBills(String studentNo, String dormitoryNo, String semester, String status) throws IOException;
}
