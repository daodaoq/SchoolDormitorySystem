package org.java.backed.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.java.backed.entity.AiQaLog;

import java.util.Map;

/**
 * AI问答服务接口
 */
public interface AiQaService extends IService<AiQaLog> {

    Map<String, Object> ask(String userId, String question);

    Page<AiQaLog> queryHistory(int pageNum, int pageSize, String userId);
}
