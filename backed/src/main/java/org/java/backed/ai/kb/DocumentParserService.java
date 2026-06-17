package org.java.backed.ai.kb;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Map;

/**
 * 文档解析服务 — 使用 Apache Tika 解析 PDF/Word/Excel/PPT/TXT/Markdown
 * 参照 ragent 的 TikaDocumentParser 设计
 */
@Slf4j
@Service
public class DocumentParserService {

    private static final Tika TIKA = new Tika();

    /**
     * 解析文档字节内容为纯文本
     *
     * @param content  文档字节数组
     * @param fileName 文件名（用于检测 MIME 类型）
     * @return 解析后的纯文本
     */
    public String parse(byte[] content, String fileName) {
        if (content == null || content.length == 0) {
            return "";
        }
        try (ByteArrayInputStream is = new ByteArrayInputStream(content)) {
            String text = TIKA.parseToString(is);
            return cleanup(text);
        } catch (Exception e) {
            log.error("Tika 解析失败: {}", fileName, e);
            throw new RuntimeException("文档解析失败: " + e.getMessage());
        }
    }

    /**
     * 清理文本：移除多余空白、控制字符
     */
    private String cleanup(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "")
                .trim();
    }
}
