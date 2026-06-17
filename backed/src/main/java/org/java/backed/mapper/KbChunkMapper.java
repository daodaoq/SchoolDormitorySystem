package org.java.backed.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.java.backed.entity.KbChunk;

import java.util.List;

@Mapper
public interface KbChunkMapper extends BaseMapper<KbChunk> {

    @Select("SELECT * FROM kb_chunk WHERE document_id = #{documentId} ORDER BY chunk_index")
    List<KbChunk> selectByDocumentId(@Param("documentId") Long documentId);

    @Select("DELETE FROM kb_chunk WHERE document_id = #{documentId}")
    void deleteByDocumentId(@Param("documentId") Long documentId);
}
