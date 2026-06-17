package org.java.backed.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.java.backed.common.PageResult;
import org.java.backed.common.Result;
import org.java.backed.entity.KbChunk;
import org.java.backed.entity.KbDocument;
import org.java.backed.service.KbDocumentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
