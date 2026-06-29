package org.java.backed.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.java.backed.entity.OperationLog;

/**
 * 操作日志服务接口
 */
public interface OperationLogService extends IService<OperationLog> {

    void saveLog(String username, String module, String action, String description, String ipAddress);
}
