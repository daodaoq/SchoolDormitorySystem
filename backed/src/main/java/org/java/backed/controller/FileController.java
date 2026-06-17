package org.java.backed.controller;

import io.minio.StatObjectResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.common.Result;
import org.java.backed.service.MinioService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 通用文件管理接口
 * 提供文件上传、下载、预览等功能
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final MinioService minioService;

    /**
     * 通用文件上传
     * @param file 上传的文件
     * @param bucket bucket 名称（如 student-photos, user-avatars）
     * @return 文件访问 URL
     */
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "common") String bucket) {
        if (file.isEmpty()) {
            return Result.badRequest("文件不能为空");
        }

        String fullBucket = minioService.getFullBucketName(bucket);
        String objectName = minioService.uploadFile(fullBucket, file);
        String url = "/api/files/" + bucket + "/" + objectName;

        return Result.ok(Map.of(
                "bucket", bucket,
                "objectName", objectName,
                "url", url
        ));
    }

    /**
     * 文件下载/预览
     * @param bucket bucket 名称
     * @param objectName 对象名
     */
    @GetMapping("/{bucket}/{objectName}")
    public ResponseEntity<byte[]> download(
            @PathVariable String bucket,
            @PathVariable String objectName) {
        String fullBucket = minioService.getFullBucketName(bucket);

        try {
            StatObjectResponse stat = minioService.getFileInfo(fullBucket, objectName);
            InputStream stream = minioService.downloadFile(fullBucket, objectName);
            byte[] bytes = stream.readAllBytes();
            stream.close();

            String contentType = stat.contentType();
            if (contentType == null) contentType = "application/octet-stream";

            // 图片类文件内联预览，其他文件下载
            String disposition = contentType.startsWith("image/")
                    ? "inline"
                    : "attachment; filename*=UTF-8''" + URLEncoder.encode(objectName, StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(bytes.length)
                    .body(bytes);
        } catch (Exception e) {
            log.error("Failed to download file: {}/{}", bucket, objectName, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/{bucket}/{objectName}")
    public Result<Void> delete(
            @PathVariable String bucket,
            @PathVariable String objectName) {
        String fullBucket = minioService.getFullBucketName(bucket);
        minioService.deleteFile(fullBucket, objectName);
        return Result.ok();
    }
}
