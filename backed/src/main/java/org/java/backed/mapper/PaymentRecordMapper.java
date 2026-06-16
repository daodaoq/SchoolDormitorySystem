package org.java.backed.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.java.backed.entity.PaymentRecord;

@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
}
