package com.eample.shop.Service;

import com.eample.shop.common.PageResult;
import com.eample.shop.vo.OrderDetailVO;
import com.eample.shop.vo.OrderVO;

public interface AdminOrderService {

    PageResult<OrderVO> list(String keyword, Integer status, Integer page, Integer size);

    OrderDetailVO detail(Long id, Integer orderType);

    void updateStatus(Long id, Integer status);

    void cancel(Long id);
}
