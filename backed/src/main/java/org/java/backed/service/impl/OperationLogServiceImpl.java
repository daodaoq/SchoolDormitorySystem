package org.java.backed.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.java.backed.entity.OperationLog;
import org.java.backed.mapper.OperationLogMapper;
import org.java.backed.service.OperationLogService;
import org.springframework.stereotype.Service;

@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog>
        implements OperationLogService {

    @Override
    public void saveLog(String operator, String module, String action, Long targetId, String detail, String ipAddress) {
        OperationLog log = new OperationLog();
        log.setOperator(operator);
        log.setModule(module);
        log.setAction(action);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setIpAddress(ipAddress);
        save(log);
    }
}
