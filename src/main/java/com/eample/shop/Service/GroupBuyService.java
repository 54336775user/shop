package com.eample.shop.Service;

import com.eample.shop.common.PageResult;
import com.eample.shop.dto.GroupBuyActivityRequestDTO;
import com.eample.shop.vo.GroupBuyActivityAdminVO;
import com.eample.shop.vo.GroupBuyActivityVO;
import com.eample.shop.vo.GroupBuyOrderVO;
import com.eample.shop.vo.GroupBuyTeamVO;

import java.util.List;

public interface GroupBuyService {

    /**
     * 根据商品ID查询拼团活动
     */
    GroupBuyActivityVO getActivityByProductId(Long productId);

    /**
     * 查询某个商品下正在拼的团
     */
    List<GroupBuyTeamVO> listTeamsByProductId(Long productId);

    /**
     * 发起拼单
     */
    Long openTeam(Long userId, Long activityId);

    /**
     * 参加已有团
     */
    Long joinTeam(Long userId, Long teamId);

    /**
     * 拼团订单支付
     */
    void payOrder(Long userId, Long orderId);

    /**
     * 查询团详情
     */
    GroupBuyTeamVO getTeamDetail(Long teamId);

    /**
     * 取消未支付订单
     */
    void cancelUnpaidOrder(Long userId, Long orderId);

    /**
     * 超时未成团处理
     */
    void expireTeam(Long teamId);

    /**
     * 查询用户的拼团订单列表
     */
    List<GroupBuyOrderVO> listOrdersByUserId(Long userId);

    // 管理端

    /**
     * 获取拼团活动列表
     * @param keyword
     * @param productId
     * @param status
     * @param page
     * @param size
     * @return
     */
    PageResult<GroupBuyActivityAdminVO> adminList(String keyword, Long productId, Integer status, Integer page, Integer size);

    /**
     * 根据id获取活动
     * @param id
     * @return
     */
    GroupBuyActivityVO getActivityById(Long id);

    /**
     * 添加活动
     * @param request
     */
    void addActivity(GroupBuyActivityRequestDTO request);

    /**
     * 更新活动
     * @param request
     */
    void updateActivity(GroupBuyActivityRequestDTO request);

    /**
     * 启动活动
     * @param id
     */
    void enableActivity(Long id);

    /**
     * 关闭活动
     * @param id
     */
    void disableActivity(Long id);
}
