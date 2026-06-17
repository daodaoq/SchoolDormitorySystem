package org.java.backed.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.ai.kb.EmbeddingService;
import org.java.backed.ai.kb.MilvusVectorStoreService;
import org.java.backed.config.RabbitMQConfig;
import org.java.backed.entity.KbChunk;
import org.java.backed.entity.KbDocument;
import org.java.backed.mapper.KbChunkMapper;
import org.java.backed.mapper.KbDocumentMapper;
import org.java.backed.service.KbDocumentService;
import org.java.backed.service.MinioService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * 知识库文档服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KbDocumentServiceImpl extends ServiceImpl<KbDocumentMapper, KbDocument>
        implements KbDocumentService {

    private final MinioService minioService;
    private final RabbitTemplate rabbitTemplate;
    private final EmbeddingService embeddingService;
    private final MilvusVectorStoreService milvusService;
    private final KbChunkMapper chunkMapper;

    private static final String KB_BUCKET = "documents";

    @Override
    @Transactional
    public KbDocument upload(MultipartFile file, String title, String description) {
        // 1. 上传到 MinIO
        String objectName = minioService.uploadFile(
                minioService.getFullBucketName(KB_BUCKET), file);
        String fileUrl = minioService.getPresignedUrl(
                minioService.getFullBucketName(KB_BUCKET), objectName);

        // 2. 保存文档元数据
        KbDocument doc = new KbDocument();
        doc.setTitle(title != null ? title : file.getOriginalFilename());
        doc.setDescription(description);
        doc.setFileName(file.getOriginalFilename());
        doc.setFileType(detectFileType(file.getOriginalFilename()));
        doc.setFileSize(file.getSize());
        doc.setFileUrl(objectName);  // 存对象名，非完整 URL
        doc.setChunkCount(0);
        doc.setStatus("PENDING");
        save(doc);

        // 3. 通过 RabbitMQ 异步触发处理流水线
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_KB_DOCUMENT,
                RabbitMQConfig.RK_KB_DOCUMENT_PROCESS,
                String.valueOf(doc.getId()));

        log.info("知识库文档上传成功: id={}, fileName={}", doc.getId(), doc.getFileName());
        return doc;
    }

    @Override
    public Page<KbDocument> page(int pageNum, int pageSize) {
        LambdaQueryWrapper<KbDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(KbDocument::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional
    public void deleteDocument(Long id) {
        KbDocument doc = getById(id);
        if (doc == null) return;

        // 1. 删除 MySQL 中的 chunks
        chunkMapper.deleteByDocumentId(id);

        // 2. 删除 Milvus 中的向量
        milvusService.deleteByDocId(id);

        // 3. 删除 MinIO 文件
        try {
            minioService.deleteFile(minioService.getFullBucketName(KB_BUCKET), doc.getFileUrl());
        } catch (Exception e) {
            log.warn("MinIO 文件删除失败（忽略）: {}", doc.getFileUrl());
        }

        // 4. 删除文档记录
        removeById(id);
        log.info("知识库文档删除完成: id={}", id);
    }

    @Override
    public List<KbChunk> getChunks(Long documentId) {
        return chunkMapper.selectByDocumentId(documentId);
    }

    @Override
    public void reprocess(Long id) {
        KbDocument doc = getById(id);
        if (doc == null) return;

        // 清空旧数据
        chunkMapper.deleteByDocumentId(id);
        milvusService.deleteByDocId(id);

        // 重新处理
        doc.setStatus("PENDING");
        doc.setChunkCount(0);
        doc.setErrorMsg(null);
        updateById(doc);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_KB_DOCUMENT,
                RabbitMQConfig.RK_KB_DOCUMENT_PROCESS,
                String.valueOf(id));
    }

    @Override
    public List<Map<String, Object>> search(String query, int topK) {
        if (!milvusService.isAvailable()) {
            log.warn("Milvus 不可用，知识库搜索降级");
            return Collections.emptyList();
        }

        try {
            // 1. 查询向量化
            float[] queryVec = embeddingService.embed(query);

            // 2. Milvus 语义搜索
            List<MilvusVectorStoreService.SearchResult> results = milvusService.search(queryVec, topK);

            // 3. 批量查文档标题
            Map<Long, String> docTitles = new HashMap<>();
            for (var r : results) {
                if (!docTitles.containsKey(r.docId())) {
                    KbDocument doc = getById(r.docId());
                    docTitles.put(r.docId(), doc != null ? doc.getTitle() : "未知文档");
                }
            }

            // 4. 组装返回结果（含文档标题供引用）
            List<Map<String, Object>> output = new ArrayList<>();
            for (var r : results) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("chunkId", r.chunkId());
                item.put("docId", r.docId());
                item.put("docTitle", docTitles.getOrDefault(r.docId(), "未知文档"));
                item.put("content", r.content());
                item.put("score", Math.round(r.score() * 10000.0) / 10000.0);
                output.add(item);
            }
            return output;
        } catch (Exception e) {
            log.error("知识库搜索失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 根据文件名检测文件类型
     */
    private String detectFileType(String fileName) {
        if (fileName == null) return "UNKNOWN";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "PDF";
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return "WORD";
        if (lower.endsWith(".xls") || lower.endsWith(".xlsx")) return "EXCEL";
        if (lower.endsWith(".ppt") || lower.endsWith(".pptx")) return "PPT";
        if (lower.endsWith(".txt")) return "TXT";
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) return "MD";
        return "UNKNOWN";
    }
}
