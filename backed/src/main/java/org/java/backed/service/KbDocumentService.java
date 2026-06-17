package org.java.backed.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.java.backed.entity.KbChunk;
import org.java.backed.entity.KbDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 知识库文档服务接口
 */
public interface KbDocumentService extends IService<KbDocument> {

    /**
     * 上传文档并触发异步处理流水线
     */
    KbDocument upload(MultipartFile file, String title, String description);

    /**
     * 分页查询文档列表
     */
    Page<KbDocument> page(int pageNum, int pageSize);

    /**
     * 删除文档（级联删除 chunks + Milvus 向量）
     */
    void deleteDocument(Long id);

    /**
     * 查看文档的分块列表
     */
    List<KbChunk> getChunks(Long documentId);

    /**
     * 重新处理文档（状态改为 PENDING 并触发流水线）
     */
    void reprocess(Long id);

    /**
     * 语义搜索知识库
     */
    List<Map<String, Object>> search(String query, int topK);
}
