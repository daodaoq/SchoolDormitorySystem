package org.java.backed.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.java.backed.entity.OperationLog;

/**
 * 操作日志服务接口
 */
public interface OperationLogService extends IService<OperationLog> {

    void saveLog(String operator, String module, String action, Long targetId, String detail, String ipAddress);
}
