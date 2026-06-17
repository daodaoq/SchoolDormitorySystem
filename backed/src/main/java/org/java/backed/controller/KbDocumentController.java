package org.java.backed.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.java.backed.ai.kb.EmbeddingService;
import org.java.backed.ai.kb.chunk.ChunkOptions;
import org.java.backed.ai.kb.chunk.FixedSizeChunkingStrategy;
import org.java.backed.common.PageResult;
import org.java.backed.common.Result;
import org.java.backed.entity.KbChunk;
import org.java.backed.entity.KbDocument;
import org.java.backed.service.KbDocumentService;
import org.java.backed.service.MinioService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库文档管理控制器
 */
@RestController
@RequestMapping("/api/kb")
@RequiredArgsConstructor
public class KbDocumentController {

    private final KbDocumentService kbDocumentService;
    private final MinioService minioService;

    /**
     * 上传文档
     */
    @PostMapping("/documents/upload")
    public Result<KbDocument> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description) {
        if (file.isEmpty()) {
            return Result.badRequest("文件不能为空");
        }
        KbDocument doc = kbDocumentService.upload(file, title, description);
        return Result.ok(doc);
    }

    /**
     * 文档列表（分页）
     */
    @GetMapping("/documents")
    public Result<PageResult<KbDocument>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<KbDocument> result = kbDocumentService.page(page, pageSize);
        return Result.ok(PageResult.from(result));
    }

    /**
     * 文档详情
     */
    @GetMapping("/documents/{id}")
    public Result<KbDocument> detail(@PathVariable Long id) {
        KbDocument doc = kbDocumentService.getById(id);
        if (doc == null) {
            return Result.fail("文档不存在");
        }
        return Result.ok(doc);
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/documents/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        kbDocumentService.deleteDocument(id);
        return Result.ok();
    }

    /**
     * 查看文档分块
     */
    @GetMapping("/documents/{id}/chunks")
    public Result<List<KbChunk>> chunks(@PathVariable Long id) {
        List<KbChunk> chunks = kbDocumentService.getChunks(id);
        return Result.ok(chunks);
    }

    /**
     * 重新处理文档
     */
    @PostMapping("/documents/{id}/reprocess")
    public Result<Void> reprocess(@PathVariable Long id) {
        kbDocumentService.reprocess(id);
        return Result.ok();
    }

    /**
     * 调试：分步测试 pipeline，返回每步状态
     */
    @GetMapping("/documents/{id}/debug-process")
    public Result<Map<String, Object>> debugProcess(@PathVariable Long id) {
        Map<String, Object> log = new LinkedHashMap<>();
        var doc = kbDocumentService.getById(id);
        if (doc == null) return Result.fail("文档不存在");
        log.put("fileName", doc.getFileName());
        log.put("fileUrl", doc.getFileUrl());

        // Step 1: MinIO
        try {
            byte[] data = minioService.download(doc.getFileUrl());
            log.put("step1_minio", "OK, " + data.length + " bytes");
        } catch (Exception e) {
            log.put("step1_minio", "FAIL: " + e.getClass().getSimpleName() + " — " + e.getMessage());
            return Result.ok(log);
        }
        return Result.ok(log);
    }

    /**
     * 最简测试 — 什么都不依赖
     */
    @PostMapping("/test")
    public Result<String> test() {
        return Result.ok("hello");
    }

    /**
     * 逐步测试：MinIO → 纯Java文本 → Chunk → Embed
     */
    @PostMapping("/documents/{id}/process-direct")
    public Result<Map<String, Object>> processDirect(@PathVariable Long id) {
        Map<String, Object> log = new LinkedHashMap<>();
        var doc = kbDocumentService.getById(id);
        if (doc == null) return Result.fail("文档不存在");
        log.put("file", doc.getFileName());

        try {
            // Step 1: MinIO
            byte[] data = minioService.download(doc.getFileUrl());
            log.put("s1_minio", data.length + " bytes");

            // Step 2: 纯Java读文本（md文件直接用UTF-8）
            String text = new String(data, java.nio.charset.StandardCharsets.UTF_8);
            log.put("s2_text", text.length() + " chars");

            // Step 3: Chunk — 只用 FixedSize，绕过 Markdown 的 bug
            var strategy = new FixedSizeChunkingStrategy();
            var chunks = strategy.chunk(text, ChunkOptions.builder().chunkSize(512).overlapSize(128).build());
            log.put("s3_chunk", strategy.name() + " → " + chunks.size() + " chunks");

            // Step 4: Embed first chunk
            if (!chunks.isEmpty()) {
                var emb = new EmbeddingService();
                var f1 = EmbeddingService.class.getDeclaredField("apiKey"); f1.setAccessible(true);
                f1.set(emb, "sk-dbda4076d5c24dc086bbfa1dca5aca27");
                var f2 = EmbeddingService.class.getDeclaredField("model"); f2.setAccessible(true);
                f2.set(emb, "text-embedding-v2");
                float[] v = emb.embed(chunks.get(0).getContent());
                log.put("s4_embed", "dim=" + v.length + ", firstChunk="
                        + chunks.get(0).getContent().substring(0, Math.min(30, chunks.get(0).getContent().length())));
            }
            log.put("status", "ALL OK!");

        } catch (Throwable e) {
            log.put("error", e.getClass().getName() + ": " + e.getMessage());
            if (e.getCause() != null) log.put("cause", e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
        }
        return Result.ok(log);
    }

    /**
     * 知识库语义搜索
     */
    @PostMapping("/search")
    public Result<List<Map<String, Object>>> search(
            @RequestBody Map<String, Object> params) {
        String query = (String) params.get("query");
        int topK = params.containsKey("topK") ? ((Number) params.get("topK")).intValue() : 5;
        if (query == null || query.trim().isEmpty()) {
            return Result.badRequest("搜索关键词不能为空");
        }
        List<Map<String, Object>> results = kbDocumentService.search(query.trim(), topK);
        return Result.ok(results);
    }
}
