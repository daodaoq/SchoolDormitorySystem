package org.java.backed.ai.kb;

import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.DataType;
import io.milvus.grpc.SearchResults;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.grpc.MutationResult;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.DropCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Milvus 向量存储服务
 * 负责向量 Collection 管理、插入、检索
 *
 * 容错策略：
 * - 启动时不要求 Milvus 立即可用（后端可能先于 Docker 启动）
 * - 连接失败后自动定时重试（初始 5 秒，之后每 30 秒）
 * - 连接成功前所有搜索操作返回空列表，不影响主流程
 */
@Slf4j
@Service
public class MilvusVectorStoreService {

    private static final String COLLECTION_NAME = "kb_chunks";
    private static final String FIELD_ID = "chunk_id";
    private static final String FIELD_DOC_ID = "doc_id";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_CHUNK_INDEX = "chunk_index";
    private static final String FIELD_EMBEDDING = "embedding";

    @Value("${milvus.host:localhost}")
    private String milvusHost;

    @Value("${milvus.port:19530}")
    private int milvusPort;

    private volatile MilvusServiceClient client;

    /** 重连中标记，避免并发重复连接 */
    private final AtomicBoolean connecting = new AtomicBoolean(false);

    /** 首次连接是否已尝试（用于日志降级） */
    private volatile boolean firstAttemptDone = false;

    @PostConstruct
    public void init() {
        // 异步尝试首次连接，不阻塞 Spring Boot 启动
        Thread initThread = new Thread(this::connectWithRetry, "milvus-init");
        initThread.setDaemon(true);
        initThread.start();
    }

    @PreDestroy
    public void destroy() {
        connecting.set(false);
        MilvusServiceClient c = client;
        client = null;
        if (c != null) {
            try {
                c.close();
            } catch (Exception ignored) {}
        }
    }

    /**
     * 带退避重试的连接逻辑
     * 初始间隔 5 秒，之后每次翻倍，最多 60 秒
     */
    private void connectWithRetry() {
        if (!connecting.compareAndSet(false, true)) {
            return; // 已有重连线程在运行
        }

        long delaySec = 5;
        final long maxDelaySec = 60;

        try {
            while (connecting.get()) {
                try {
                    doConnect();
                    log.info("Milvus 连接成功: {}:{}", milvusHost, milvusPort);
                    firstAttemptDone = true;
                    return; // 成功，退出重试循环
                } catch (Exception e) {
                    if (!firstAttemptDone) {
                        log.warn("Milvus 首次连接失败 ({}:{})，{} 秒后重试: {}",
                                milvusHost, milvusPort, delaySec, e.getMessage());
                        firstAttemptDone = true;
                    } else {
                        log.debug("Milvus 重连失败，{} 秒后重试: {}", delaySec, e.getMessage());
                    }
                }

                // 退避等待
                try {
                    TimeUnit.SECONDS.sleep(delaySec);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    connecting.set(false);
                    return;
                }

                delaySec = Math.min(delaySec * 2, maxDelaySec);
            }
        } finally {
            connecting.set(false);
        }
    }

    /**
     * 定时重连检查（每 30 秒一次）
     * 如果 client 为 null 且无其他线程在重连，触发重连
     */
    @Scheduled(fixedDelay = 30_000, initialDelay = 60_000)
    public void scheduledReconnect() {
        if (client == null && connecting.compareAndSet(false, true)) {
            try {
                log.info("定时重连 Milvus: {}:{}", milvusHost, milvusPort);
                doConnect();
                log.info("定时重连 Milvus 成功: {}:{}", milvusHost, milvusPort);
            } catch (Exception e) {
                log.debug("定时重连 Milvus 仍失败: {}", e.getMessage());
            } finally {
                connecting.set(false);
            }
        }
    }

    /**
     * 实际执行连接 + 确保 Collection 存在
     */
    private synchronized void doConnect() {
        MilvusServiceClient oldClient = this.client;
        MilvusServiceClient newClient = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost(milvusHost)
                        .withPort(milvusPort)
                        .build()
        );

        // 测试连接
        HasCollectionParam hasParam = HasCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .build();
        newClient.hasCollection(hasParam);

        // 连接成功 → 替换旧 client
        this.client = newClient;
        if (oldClient != null) {
            try { oldClient.close(); } catch (Exception ignored) {}
        }

        ensureCollection();
    }

    /**
     * 确保 Collection 存在，不存在则创建
     */
    private void ensureCollection() {
        if (client == null) return;

        HasCollectionParam hasParam = HasCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .build();
        boolean exists = client.hasCollection(hasParam).getData();

        if (!exists) {
            // 定义字段
            FieldType idField = FieldType.newBuilder()
                    .withName(FIELD_ID)
                    .withDataType(DataType.VarChar)
                    .withMaxLength(64)
                    .withPrimaryKey(true)
                    .build();

            FieldType docIdField = FieldType.newBuilder()
                    .withName(FIELD_DOC_ID)
                    .withDataType(DataType.Int64)
                    .build();

            FieldType contentField = FieldType.newBuilder()
                    .withName(FIELD_CONTENT)
                    .withDataType(DataType.VarChar)
                    .withMaxLength(65535)
                    .build();

            FieldType chunkIndexField = FieldType.newBuilder()
                    .withName(FIELD_CHUNK_INDEX)
                    .withDataType(DataType.Int32)
                    .build();

            FieldType embeddingField = FieldType.newBuilder()
                    .withName(FIELD_EMBEDDING)
                    .withDataType(DataType.FloatVector)
                    .withDimension(1536)  // text-embedding-v2 实际维度
                    .build();

            CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .withDescription("知识库文档分块向量")
                    .addFieldType(idField)
                    .addFieldType(docIdField)
                    .addFieldType(contentField)
                    .addFieldType(chunkIndexField)
                    .addFieldType(embeddingField)
                    .build();

            client.createCollection(createParam);

            // 创建 IVF_FLAT 索引
            CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .withFieldName(FIELD_EMBEDDING)
                    .withIndexType(IndexType.IVF_FLAT)
                    .withMetricType(MetricType.COSINE)
                    .withExtraParam("{\"nlist\":128}")
                    .build();
            client.createIndex(indexParam);

            log.info("Milvus Collection 创建成功: {}", COLLECTION_NAME);
        }

        // 加载到内存
        LoadCollectionParam loadParam = LoadCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .build();
        client.loadCollection(loadParam);
    }

    /**
     * 批量插入向量
     *
     * @param chunks    分块数据（chunkId, docId, content, embedding）
     */
    public void insert(List<ChunkVector> chunks) {
        if (client == null || chunks.isEmpty()) return;

        int batchSize = 100;
        for (int i = 0; i < chunks.size(); i += batchSize) {
            int end = Math.min(i + batchSize, chunks.size());
            List<ChunkVector> batch = chunks.subList(i, end);

            List<InsertParam.Field> fields = new ArrayList<>();
            fields.add(new InsertParam.Field(FIELD_ID,
                    batch.stream().map(ChunkVector::chunkId).collect(Collectors.toList())));
            fields.add(new InsertParam.Field(FIELD_DOC_ID,
                    batch.stream().map(ChunkVector::docId).collect(Collectors.toList())));
            fields.add(new InsertParam.Field(FIELD_CONTENT,
                    batch.stream().map(ChunkVector::content).collect(Collectors.toList())));
            fields.add(new InsertParam.Field(FIELD_CHUNK_INDEX,
                    batch.stream().map(c -> c.chunkIndex()).collect(Collectors.toList())));
            fields.add(new InsertParam.Field(FIELD_EMBEDDING,
                    batch.stream().map(ChunkVector::embedding).collect(Collectors.toList())));

            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(COLLECTION_NAME)
                    .withFields(fields)
                    .build();

            R<MutationResult> resp = client.insert(insertParam);
            if (resp.getStatus() != 0) {
                log.error("Milvus 插入失败: {}", resp.getMessage());
                throw new RuntimeException("向量插入失败: " + resp.getMessage());
            }
        }
        log.info("Milvus 批量插入完成: {} 条向量", chunks.size());
    }

    /**
     * 语义搜索
     *
     * @param queryVector 查询向量
     * @param topK        返回前 K 条
     * @return 搜索结果
     */
    public List<SearchResult> search(float[] queryVector, int topK) {
        if (client == null) return Collections.emptyList();

        List<String> outFields = List.of(FIELD_DOC_ID, FIELD_CONTENT, FIELD_CHUNK_INDEX);

        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withVectorFieldName(FIELD_EMBEDDING)
                .withMetricType(MetricType.COSINE)
                .withOutFields(outFields)
                .withTopK(topK)
                .withFloatVectors(Collections.singletonList(asList(queryVector)))
                .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
                .build();

        R<SearchResults> resp = client.search(searchParam);
        if (resp.getStatus() != 0) {
            log.error("Milvus 检索失败: {}", resp.getMessage());
            return Collections.emptyList();
        }

        SearchResultsWrapper wrapper = new SearchResultsWrapper(resp.getData().getResults());
        List<SearchResult> results = new ArrayList<>();

        for (int i = 0; i < wrapper.getIDScore(0).size(); i++) {
            SearchResultsWrapper.IDScore idScore = wrapper.getIDScore(0).get(i);
            String content = (String) wrapper.getFieldData(FIELD_CONTENT, 0).get(i);
            Long docId = (Long) wrapper.getFieldData(FIELD_DOC_ID, 0).get(i);
            Integer chunkIndex = (Integer) wrapper.getFieldData(FIELD_CHUNK_INDEX, 0).get(i);

            results.add(new SearchResult(
                    idScore.getStrID(),
                    docId,
                    content,
                    chunkIndex != null ? chunkIndex : 0,
                    idScore.getScore()
            ));
        }

        return results;
    }

    /**
     * 删除文档的所有向量
     */
    public void deleteByDocId(Long docId) {
        if (client == null) return;
        String expr = FIELD_DOC_ID + " == " + docId;
        DeleteParam deleteParam = DeleteParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withExpr(expr)
                .build();
        client.delete(deleteParam);
        log.info("Milvus 删除完成: docId={}", docId);
    }

    /**
     * 检查 Milvus 是否可用
     */
    public boolean isAvailable() {
        return client != null;
    }

    private List<Float> asList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float v : array) list.add(v);
        return list;
    }

    // ==================== 数据类 ====================

    /**
     * 待插入的向量数据
     */
    public record ChunkVector(String chunkId, Long docId, String content, int chunkIndex, List<Float> embedding) {}

    /**
     * 检索结果
     */
    public record SearchResult(String chunkId, Long docId, String content, int chunkIndex, float score) {}
}
