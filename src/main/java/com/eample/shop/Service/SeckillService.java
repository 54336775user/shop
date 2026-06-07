package com.eample.shop.Service;

import com.eample.shop.dto.SeckillBuyDTO;
import com.eample.shop.vo.SeckillOrderVO;
import com.eample.shop.vo.SeckillProductVO;

import java.util.List;

public interface SeckillService {

    /**
     * 秒杀商品列表
     */
    List<SeckillProductVO> list();

    /**
     * 秒杀商品详情
     */
    SeckillProductVO detail(Long productId);

    /**
     * 创建秒杀请求令牌
     */
    String createToken(Long userId, Long productId);

    /**
     * 发起秒杀
     */
    String buy(Long userId, Long productId, SeckillBuyDTO request);

    /**
     * 查询秒杀结果
     */
    SeckillOrderVO result(Long userId, String requestId);
    /**
     * 预热加载Redis库存
     */
    void warmupStock();

    /**
     * 预热加载Redis库存
     * @param productId
     */
    void warmupStockByProductId(Long productId);

    /**
     * 刷新秒杀商品列表 Redis 缓存
     */
    void refreshProductListCache();

    /** 查询 buy 是否处于降级状态 */
    boolean isBuyDegraded();

    /** 开启 buy 降级 */
    void enableBuyDegrade();

    /** 关闭 buy 降级 */
    void disableBuyDegrade();
}
