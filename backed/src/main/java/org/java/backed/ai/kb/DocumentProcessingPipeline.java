package org.java.backed.ai.kb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.ai.kb.chunk.ChunkOptions;
import org.java.backed.ai.kb.chunk.ChunkResult;
import org.java.backed.ai.kb.chunk.FixedSizeChunkingStrategy;
import org.java.backed.entity.KbChunk;
import org.java.backed.entity.KbDocument;
import org.java.backed.mapper.KbChunkMapper;
import org.java.backed.mapper.KbDocumentMapper;
import org.java.backed.service.MinioService;
import org.springframework.stereotype.Service;

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
    private final ChunkQualityEvaluator qualityEvaluator;
    private final EmbeddingService embeddingService;
    private final MilvusVectorStoreService milvusService;
    private final KbDocumentMapper documentMapper;
    private final KbChunkMapper chunkMapper;

    /**
     * 处理文档（由 RabbitMQ 消费者调用）
     * 注意：不加 @Transactional，因为每步可能耗时较长，
     * 且异常时需要保留 FAILED 状态不被回滚
     */
    public void process(Long documentId) {
        KbDocument doc = documentMapper.selectById(documentId);
        if (doc == null) {
            log.warn("文档不存在: id={}", documentId);
            return;
        }

        try {
            log.info("=== 开始处理文档 [id={}, file={}] ===", documentId, doc.getFileName());

            // 1. PROCESSING
            KbDocument update = new KbDocument();
            update.setId(doc.getId());
            update.setStatus("PROCESSING");
            documentMapper.updateById(update);
            log.info("[1/6] 状态→PROCESSING ✓");

            // 2. MinIO 下载
            log.info("[2/6] MinIO 下载: {}", doc.getFileUrl());
            byte[] content = minioService.download(doc.getFileUrl());
            log.info("[2/6] MinIO 下载完成: {} bytes", content.length);

            // 3. 解析
            log.info("[3/6] Tika 解析...");
            String text = parserService.parse(content, doc.getFileName());
            log.info("[3/6] 解析完成: {} 字符", text.length());

            // 4. 分块（暂时全部用 FixedSize，Markdown 策略有递归 bug）
            var strategy = new org.java.backed.ai.kb.chunk.FixedSizeChunkingStrategy();
            log.info("[4/6] 分块策略: {}", strategy.name());
            ChunkOptions chunkOpts = ChunkOptions.builder()
                    .chunkSize(512).overlapSize(128).build();
            List<ChunkResult> chunkResults = strategy.chunk(text, chunkOpts);
            log.info("[4/6] 原始分块: {} 个", chunkResults.size());

            chunkResults = qualityEvaluator.evaluate(chunkResults);
            log.info("[4/6] 质量过滤后: {} 个", chunkResults.size());

            // 5. 向量化
            List<String> chunkTexts = chunkResults.stream()
                    .map(ChunkResult::getContent)
                    .collect(Collectors.toList());
            log.info("[5/6] Embedding 开始: {} 个文本...", chunkTexts.size());
            List<float[]> vectors = embeddingService.embedBatch(chunkTexts);
            log.info("[5/6] Embedding 完成: {} 个向量, dim={}",
                    vectors.size(), vectors.isEmpty() ? "?" : vectors.get(0).length);

            // 6. 存储分块元数据到 MySQL
            List<KbChunk> chunks = new ArrayList<>();
            for (int i = 0; i < chunkResults.size(); i++) {
                KbChunk chunk = new KbChunk();
                chunk.setDocumentId(documentId);
                chunk.setChunkId(chunkResults.get(i).getChunkId());
                chunk.setChunkIndex(chunkResults.get(i).getIndex());
                chunk.setContent(chunkResults.get(i).getContent());
                chunk.setTokenCount(estimateTokens(chunkResults.get(i).getContent()));
                chunks.add(chunk);
            }
            for (KbChunk chunk : chunks) {
                chunkMapper.insert(chunk);
            }
            log.info("[6/6] MySQL 存储完成: {} 条 chunks", chunks.size());

            // 7. 存储向量到 Milvus
            log.info("[7/6] Milvus 存储开始...");
            List<MilvusVectorStoreService.ChunkVector> milvusChunks = new ArrayList<>();
            for (int i = 0; i < chunkResults.size(); i++) {
                milvusChunks.add(new MilvusVectorStoreService.ChunkVector(
                        chunkResults.get(i).getChunkId(),
                        documentId,
                        chunkResults.get(i).getContent(),
                        chunkResults.get(i).getIndex(),
                        asList(vectors.get(i))
                ));
            }
            milvusService.insert(milvusChunks);
            log.info("[7/6] Milvus 存储完成");

            // 8. COMPLETED
            KbDocument done = new KbDocument();
            done.setId(documentId);
            done.setStatus("COMPLETED");
            done.setChunkCount(chunks.size());
            documentMapper.updateById(done);
            log.info("=== 文档处理完成: id={}, 分块={} ===", documentId, chunks.size());

        } catch (Exception e) {
            log.error("文档处理失败: id={}", documentId, e);
            KbDocument failUpdate = new KbDocument();
            failUpdate.setId(doc.getId());
            failUpdate.setStatus("FAILED");
            failUpdate.setErrorMsg(e.getClass().getSimpleName() + ": " + e.getMessage());
            documentMapper.updateById(failUpdate);
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
