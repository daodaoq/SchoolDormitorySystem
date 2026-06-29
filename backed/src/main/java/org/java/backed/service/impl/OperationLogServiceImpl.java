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
    public void saveLog(String username, String module, String action, String description, String ipAddress) {
        OperationLog log = new OperationLog();
        log.setUsername(username);
        log.setModule(module);
        log.setAction(action);
        log.setDescription(description);
        log.setIpAddress(ipAddress);
        log.setStatus("SUCCESS");
        save(log);
    }
}
