package org.java.backed.ai.kb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.entity.KbChunk;
import org.java.backed.entity.KbDocument;
import org.java.backed.mapper.KbChunkMapper;
import org.java.backed.mapper.KbDocumentMapper;
import org.java.backed.service.MinioService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档处理流水线
 * 串联 下载文件 → 解析文本 → 分块 → 向量化 → 存入 Milvus + MySQL 全流程
 * 参照 ragent 的 ParserNode → ChunkerNode → IndexerNode 流水线设计
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingPipeline {

    private final MinioService minioService;
    private final DocumentParserService parserService;
    private final TextChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final MilvusVectorStoreService milvusService;
    private final KbDocumentMapper documentMapper;
    private final KbChunkMapper chunkMapper;

    /**
     * 异步处理文档（上传完成后调用）
     */
    @Async
    @Transactional
    public void processAsync(Long documentId) {
        KbDocument doc = documentMapper.selectById(documentId);
        if (doc == null) {
            log.warn("文档不存在: id={}", documentId);
            return;
        }

        try {
            // 1. 更新状态为处理中
            doc.setStatus("PROCESSING");
            documentMapper.updateById(doc);

            // 2. 从 MinIO 下载文件
            log.info("开始处理文档: id={}, fileName={}", documentId, doc.getFileName());
            byte[] content = minioService.download(doc.getFileUrl());
            if (content == null || content.length == 0) {
                throw new RuntimeException("文件内容为空");
            }

            // 3. 解析文本
            String text = parserService.parse(content, doc.getFileName());
            if (text.trim().isEmpty()) {
                throw new RuntimeException("文档解析后无有效文本");
            }

            // 4. 分块
            List<TextChunkingService.ChunkResult> chunkResults = chunkingService.chunk(text);
            if (chunkResults.isEmpty()) {
                throw new RuntimeException("文档分块为空");
            }

            // 5. 向量化
            List<String> chunkTexts = chunkResults.stream()
                    .map(TextChunkingService.ChunkResult::getContent)
                    .collect(Collectors.toList());
            List<float[]> vectors = embeddingService.embedBatch(chunkTexts);

            if (vectors.size() != chunkResults.size()) {
                throw new RuntimeException("向量数量与分块数量不匹配");
            }

            // 6. 存储分块元数据到 MySQL
            List<KbChunk> chunks = new ArrayList<>();
            for (int i = 0; i < chunkResults.size(); i++) {
                KbChunk chunk = new KbChunk();
                chunk.setDocumentId(documentId);
                chunk.setChunkIndex(chunkResults.get(i).getIndex());
                chunk.setContent(chunkResults.get(i).getContent());
                chunk.setTokenCount(estimateTokens(chunkResults.get(i).getContent()));
                chunks.add(chunk);
            }
            for (KbChunk chunk : chunks) {
                chunkMapper.insert(chunk);
            }

            // 7. 存储向量到 Milvus
            List<MilvusVectorStoreService.ChunkVector> milvusChunks = new ArrayList<>();
            for (int i = 0; i < chunkResults.size(); i++) {
                milvusChunks.add(new MilvusVectorStoreService.ChunkVector(
                        chunkResults.get(i).getChunkId(),
                        documentId,
                        chunkResults.get(i).getContent(),
                        asList(vectors.get(i))
                ));
            }
            milvusService.insert(milvusChunks);

            // 8. 更新文档状态为完成
            doc.setStatus("COMPLETED");
            doc.setChunkCount(chunks.size());
            documentMapper.updateById(doc);

            log.info("文档处理完成: id={}, 分块数={}", documentId, chunks.size());

        } catch (Exception e) {
            log.error("文档处理失败: id={}", documentId, e);
            doc.setStatus("FAILED");
            doc.setErrorMsg(e.getMessage());
            documentMapper.updateById(doc);
        }
    }

    /**
     * 估算 Token 数（ASCII 4字符≈1token，中文 1字符≈1token）
     */
    private int estimateTokens(String text) {
        int ascii = 0, cjk = 0;
        for (char c : text.toCharArray()) {
            if (c <= 127) ascii++;
            else cjk++;
        }
        return ascii / 4 + cjk;
    }

    private List<Float> asList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float v : array) list.add(v);
        return list;
    }
}
