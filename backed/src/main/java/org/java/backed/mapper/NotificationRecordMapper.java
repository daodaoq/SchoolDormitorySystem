package org.java.backed.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.java.backed.entity.NotificationRecord;

@Mapper
public interface NotificationRecordMapper extends BaseMapper<NotificationRecord> {
}
