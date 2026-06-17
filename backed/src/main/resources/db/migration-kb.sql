-- ============================================
-- 知识库 + AI 问答 增量迁移（不删表，只追加）
-- 用法: mysql -u root -p123456 school_dormitory < migration-kb.sql
-- ============================================

-- 知识库文档表（如已存在则跳过）
CREATE TABLE IF NOT EXISTS kb_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    title VARCHAR(200) NOT NULL COMMENT '文档标题',
    description VARCHAR(1000) DEFAULT NULL COMMENT '文档描述',
    file_name VARCHAR(200) NOT NULL COMMENT '原始文件名',
    file_type VARCHAR(50) NOT NULL COMMENT '文件类型: PDF/WORD/EXCEL/PPT/TXT/MD',
    file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
    file_url VARCHAR(500) DEFAULT NULL COMMENT 'MinIO 对象名',
    chunk_count INT DEFAULT 0 COMMENT '分块数量',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/COMPLETED/FAILED',
    error_msg VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_kb_doc_status (status),
    INDEX idx_kb_doc_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档表';

-- 知识库分块表
CREATE TABLE IF NOT EXISTS kb_chunk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    document_id BIGINT NOT NULL COMMENT '关联文档ID',
    chunk_index INT NOT NULL COMMENT '分块序号(从0开始)',
    content TEXT NOT NULL COMMENT '分块文本内容',
    token_count INT DEFAULT NULL COMMENT 'Token数量估算',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_kb_chunk_doc (document_id),
    INDEX idx_kb_chunk_index (document_id, chunk_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库分块表';

-- 追加 AI 问答和知识库菜单（如果已存在则忽略）
INSERT IGNORE INTO sys_menu (id, parent_id, menu_name, menu_type, path, icon, permission_code, sort_order, visible) VALUES
(13, 0, 'AI问答', 'MENU', '/ai-qa', 'RobotOutlined', 'ai:view', 10, 1),
(14, 0, '知识库', 'MENU', '/knowledge-base', 'BookOutlined', 'kb:view', 11, 1);

-- 追加角色菜单关联
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(1, 13), (1, 14),
(2, 13), (2, 14),
(3, 13);
