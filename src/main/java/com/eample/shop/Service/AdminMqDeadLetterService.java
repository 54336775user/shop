package com.eample.shop.Service;

import com.eample.shop.common.PageResult;
import com.eample.shop.vo.MqDeadLetterVO;

public interface AdminMqDeadLetterService {

    /** 待处理数量（给管理端红点用） */
    long pendingCount();

    /** 分页列表 */
    PageResult<MqDeadLetterVO> list(Integer status, Integer page, Integer size);

    /** 单条详情 */
    MqDeadLetterVO detail(Long id);

    /** 标记已处理 */
    void markDone(Long id, Long adminId, String remark);
}
