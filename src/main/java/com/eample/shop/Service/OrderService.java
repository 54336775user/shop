package com.eample.shop.Service;
import com.eample.shop.dto.OrderCreateDTO;
import com.eample.shop.vo.OrderDetailVO;
import com.eample.shop.vo.OrderVO;
import java.util.List;
public interface OrderService {
    /**
     * 创建订单
     * @param userId
     * @param request
     * @return
     */
    Long create(Long userId, OrderCreateDTO request);

    /**
     * 返回订单列表
     * @param userId
     * @return
     */
    List<OrderVO> list(Long userId);

    /**
     * 订单详情
     * @param userId
     * @param orderId
     * @return
     */
    OrderDetailVO detail(Long userId, Long orderId, Integer orderType);

    /**
     * 取消订单
     * @param userId
     * @param orderId
     * @param orderType 1普通 2秒杀，可为空
     */
    void cancel(Long userId, Long orderId, Integer orderType);

    /**
     * 创建订单token
     * @param userId
     * @return
     */
    String createToken(Long userId);

    /**
     * 待支付订单超时后取消（幂等：未超时或非待支付时不处理）
     */
    void expireUnpaidOrder(Long userId, Long orderId, Integer orderType);

    /**
     * 模拟支付
     * @param userId
     * @param orderId
     */
    void pay(Long userId, Long orderId, Integer orderType);
}