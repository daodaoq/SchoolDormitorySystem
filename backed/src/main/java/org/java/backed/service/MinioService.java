package org.java.backed.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java.backed.config.MinioConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 对象存储服务
 * 封装文件上传、下载、删除、URL 生成等通用操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    /**
     * 确保 bucket 存在，不存在则自动创建
     */
    public void ensureBucket(String bucket) {
        try {
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket created: {}", bucket);
            }
        } catch (Exception e) {
            log.error("Failed to ensure bucket: {}", bucket, e);
            throw new RuntimeException("MinIO bucket 操作失败: " + e.getMessage());
        }
    }

    /**
     * 上传文件，返回对象名（UUID + 原始扩展名）
     */
    public String uploadFile(String bucket, MultipartFile file) {
        ensureBucket(bucket);
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String objectName = UUID.randomUUID().toString() + extension;

        try (InputStream stream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(stream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            log.info("File uploaded to MinIO: {}/{} ({} bytes)", bucket, objectName, file.getSize());
            return objectName;
        } catch (Exception e) {
            log.error("Failed to upload file to {}/{}", bucket, objectName, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传文件（通过 InputStream），返回对象名
     */
    public String uploadFile(String bucket, String originalFilename, InputStream stream,
                             long size, String contentType) {
        ensureBucket(bucket);
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectName = UUID.randomUUID().toString() + extension;

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(stream, size, -1)
                            .contentType(contentType)
                            .build());
            log.info("File uploaded to MinIO: {}/{} ({} bytes)", bucket, objectName, size);
            return objectName;
        } catch (Exception e) {
            log.error("Failed to upload file to {}/{}", bucket, objectName, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件，返回 InputStream（调用方负责关闭）
     */
    public InputStream downloadFile(String bucket, String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            log.error("Failed to download file: {}/{}", bucket, objectName, e);
            throw new RuntimeException("文件下载失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件信息（大小、ContentType）
     */
    public StatObjectResponse getFileInfo(String bucket, String objectName) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            log.error("Failed to stat file: {}/{}", bucket, objectName, e);
            throw new RuntimeException("获取文件信息失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     */
    public void deleteFile(String bucket, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build());
            log.info("File deleted from MinIO: {}/{}", bucket, objectName);
        } catch (Exception e) {
            log.error("Failed to delete file: {}/{}", bucket, objectName, e);
            throw new RuntimeException("文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 生成预签名访问 URL（有效期默认 7 天）
     */
    public String getPresignedUrl(String bucket, String objectName) {
        return getPresignedUrl(bucket, objectName, 7, TimeUnit.DAYS);
    }

    /**
     * 生成预签名访问 URL
     */
    public String getPresignedUrl(String bucket, String objectName, int duration, TimeUnit unit) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectName)
                            .expiry(duration, unit)
                            .build());
        } catch (Exception e) {
            log.error("Failed to generate presigned URL: {}/{}", bucket, objectName, e);
            throw new RuntimeException("生成访问链接失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件内容为字节数组
     */
    public byte[] download(String objectName) {
        String bucket = getFullBucketName("documents");
        try (InputStream is = downloadFile(bucket, objectName)) {
            return is.readAllBytes();
        } catch (Exception e) {
            log.error("Failed to download file: {}/{}", bucket, objectName, e);
            throw new RuntimeException("文件下载失败: " + e.getMessage());
        }
    }

    /**
     * 从 MinIO URL 中提取对象名
     */
    public String extractObjectName(String fileUrl) {
        if (fileUrl == null) return null;
        // URL 格式: http://localhost:9000/dormitory-documents/abc123.pdf
        // 提取最后一个 / 之后的部分
        int lastSlash = fileUrl.lastIndexOf('/');
        return lastSlash >= 0 ? fileUrl.substring(lastSlash + 1) : fileUrl;
    }

    /**
     * 获取完整的 bucket 名称（带前缀）
     */
    public String getFullBucketName(String bucket) {
        return minioConfig.getBucketPrefix() != null && !minioConfig.getBucketPrefix().isEmpty()
                ? minioConfig.getBucketPrefix() + "-" + bucket
                : bucket;
    }
}
