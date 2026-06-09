package com.eample.shop.Service.impl;

import com.eample.shop.Service.GroupBuyService;
import com.eample.shop.common.PageResult;
import com.eample.shop.dto.GroupBuyActivityRequestDTO;
import com.eample.shop.entity.GroupBuyActivity;
import com.eample.shop.entity.GroupBuyMember;
import com.eample.shop.entity.GroupBuyOrder;
import com.eample.shop.entity.GroupBuyTeam;
import com.eample.shop.mapper.GroupBuyActivityMapper;
import com.eample.shop.mapper.GroupBuyMemberMapper;
import com.eample.shop.mapper.GroupBuyOrderMapper;
import com.eample.shop.mapper.GroupBuyTeamMapper;
import com.eample.shop.entity.Product;
import com.eample.shop.mapper.ProductMapper;
import com.eample.shop.vo.GroupBuyActivityAdminVO;
import com.eample.shop.vo.GroupBuyActivityVO;
import com.eample.shop.vo.GroupBuyOrderVO;
import com.eample.shop.vo.GroupBuyTeamVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupBuyServiceImpl implements GroupBuyService {

    private static final int ACTIVITY_STATUS_ENABLED = 1;
    private static final int TEAM_STATUS_ONGOING = 0;
    private static final int TEAM_STATUS_SUCCESS = 1;
    private static final int TEAM_STATUS_FAILED = 2;

    private static final int MEMBER_ROLE_LEADER = 1;
    private static final int MEMBER_ROLE_MEMBER = 2;

    private static final int MEMBER_STATUS_UNPAID = 0;
    private static final int MEMBER_STATUS_PAID = 1;
    private static final int MEMBER_STATUS_CANCELLED = 2;
    private static final int MEMBER_STATUS_REFUNDED = 3;

    private static final int ORDER_STATUS_UNPAID = 0;
    private static final int ORDER_STATUS_PAID_WAITING = 1;
    private static final int ORDER_STATUS_SUCCESS = 2;
    private static final int ORDER_STATUS_CANCELLED = 4;
    private static final int ORDER_STATUS_REFUNDED = 5;
    private static final int PAYMENT_TIMEOUT_MINUTES = 15;

    private final GroupBuyActivityMapper groupBuyActivityMapper;
    private final GroupBuyTeamMapper groupBuyTeamMapper;
    private final GroupBuyMemberMapper groupBuyMemberMapper;
    private final GroupBuyOrderMapper groupBuyOrderMapper;
    private final ProductMapper productMapper;

    @Override
    public GroupBuyActivityVO getActivityByProductId(Long productId) {
        GroupBuyActivity activity = groupBuyActivityMapper.findActiveByProductId(productId);
        if (activity == null) {
            return null;
        }
        return toActivityVO(activity);
    }

    @Override
    public List<GroupBuyTeamVO> listTeamsByProductId(Long productId) {
        LocalDateTime now = LocalDateTime.now();
        List<GroupBuyTeam> teams = groupBuyTeamMapper.listActiveByProductId(productId, now, 20);
        return teams.stream().map(this::toTeamVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long openTeam(Long userId, Long activityId) {
        if (userId == null) {
            log.error("用户未登录");
            throw new RuntimeException("用户未登录");
        }
        if (activityId == null) {
            log.error("拼团活动不存在");
            throw new RuntimeException("拼团活动不存在");
        }

        GroupBuyActivity activity = groupBuyActivityMapper.findById(activityId);
        if (activity == null || !Integer.valueOf(ACTIVITY_STATUS_ENABLED).equals(activity.getStatus())) {
            log.error("拼团活动不存在或已下架");
            throw new RuntimeException("拼团活动不存在或已下架");
        }

        LocalDateTime now = LocalDateTime.now();
        if (activity.getStartTime() != null && now.isBefore(activity.getStartTime())) {
            log.error("拼团活动暂未开始");
            throw new RuntimeException("拼团活动暂未开始");
        }
        if (activity.getEndTime() != null && now.isAfter(activity.getEndTime())) {
            log.error("拼团活动已结束");
            throw new RuntimeException("拼团活动已结束");
        }

        Product product = productMapper.findById(activity.getProductId());
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            log.error("商品已下架");
            throw new RuntimeException("商品已下架");
        }
        if (product.getIsFlashSale() != null && product.getIsFlashSale() == 1) {
            log.error("秒杀商品暂不支持拼团");
            throw new RuntimeException("秒杀商品暂不支持拼团");
        }

        if (activity.getStock() == null || activity.getStock() <= 0) {
            log.error("拼团库存不足");
            throw new RuntimeException("拼团库存不足");
        }
        if (activity.getGroupPrice() == null || activity.getGroupPrice().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("拼团价格异常");
            throw new RuntimeException("拼团价格异常");
        }

        GroupBuyTeam team = new GroupBuyTeam();
        team.setTeamNo(generateTeamNo(userId));
        team.setActivityId(activity.getId());
        team.setProductId(activity.getProductId());
        team.setLeaderUserId(userId);
        Integer requiredSize = activity.getRequiredSize();
        team.setRequiredSize(requiredSize == null || requiredSize < 2 ? 2 : requiredSize);
        team.setPaidSize(0);
        team.setStatus(TEAM_STATUS_ONGOING);
        Integer durationHours = activity.getDurationHours();
        int safeDurationHours = durationHours == null || durationHours <= 0 ? 24 : durationHours;
        team.setExpireTime(now.plusHours(safeDurationHours));
        team.setCreateTime(now);
        team.setUpdateTime(now);
        groupBuyTeamMapper.insert(team);

        GroupBuyOrder order = new GroupBuyOrder();
        order.setOrderNo(generateOrderNo(userId));
        order.setUserId(userId);
        order.setTeamId(team.getId());
        order.setActivityId(activity.getId());
        order.setProductId(product.getId());
        order.setProductName(product.getName());
        order.setProductImage(product.getImage());
        order.setGroupPrice(activity.getGroupPrice());
        order.setQuantity(1);
        order.setTotalAmount(activity.getGroupPrice());
        order.setStatus(ORDER_STATUS_UNPAID);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        groupBuyOrderMapper.insert(order);

        GroupBuyMember member = new GroupBuyMember();
        member.setTeamId(team.getId());
        member.setUserId(userId);
        member.setOrderId(order.getId());
        member.setRole(MEMBER_ROLE_LEADER);
        member.setStatus(MEMBER_STATUS_UNPAID);
        member.setJoinTime(now);
        member.setPayTime(null);
        groupBuyMemberMapper.insert(member);

        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long joinTeam(Long userId, Long teamId) {
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        if (teamId == null) {
            throw new RuntimeException("拼团不存在");
        }

        GroupBuyTeam team = groupBuyTeamMapper.findByIdForUpdate(teamId);
        if (team == null) {
            throw new RuntimeException("拼团不存在");
        }
        if (!Integer.valueOf(TEAM_STATUS_ONGOING).equals(team.getStatus())) {
            throw new RuntimeException("该拼团已结束");
        }
        if (team.getExpireTime() != null && LocalDateTime.now().isAfter(team.getExpireTime())) {
            throw new RuntimeException("该拼团已超时");
        }
        if (team.getRequiredSize() != null && team.getPaidSize() != null
                && team.getPaidSize() >= team.getRequiredSize()) {
            throw new RuntimeException("该拼团已成团");
        }

        GroupBuyMember existed = groupBuyMemberMapper.findByTeamIdAndUserId(teamId, userId);
        if (existed != null) {
            throw new RuntimeException("你已经参与过该拼团");
        }

        GroupBuyActivity activity = groupBuyActivityMapper.findById(team.getActivityId());
        if (activity == null || !Integer.valueOf(ACTIVITY_STATUS_ENABLED).equals(activity.getStatus())) {
            throw new RuntimeException("拼团活动不存在或已下架");
        }

        Product product = productMapper.findById(team.getProductId());
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            throw new RuntimeException("商品已下架");
        }
        if (product.getIsFlashSale() != null && product.getIsFlashSale() == 1) {
            throw new RuntimeException("秒杀商品暂不支持拼团");
        }

        if (activity.getGroupPrice() == null || activity.getGroupPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("拼团价格异常");
        }

        LocalDateTime now = LocalDateTime.now();
        GroupBuyOrder order = new GroupBuyOrder();
        order.setOrderNo(generateOrderNo(userId));
        order.setUserId(userId);
        order.setTeamId(team.getId());
        order.setActivityId(activity.getId());
        order.setProductId(product.getId());
        order.setProductName(product.getName());
        order.setProductImage(product.getImage());
        order.setGroupPrice(activity.getGroupPrice());
        order.setQuantity(1);
        order.setTotalAmount(activity.getGroupPrice());
        order.setStatus(ORDER_STATUS_UNPAID);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        groupBuyOrderMapper.insert(order);

        GroupBuyMember member = new GroupBuyMember();
        member.setTeamId(team.getId());
        member.setUserId(userId);
        member.setOrderId(order.getId());
        member.setRole(MEMBER_ROLE_MEMBER);
        member.setStatus(MEMBER_STATUS_UNPAID);
        member.setJoinTime(now);
        member.setPayTime(null);
        groupBuyMemberMapper.insert(member);

        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long userId, Long orderId) {
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        if (orderId == null) {
            throw new RuntimeException("订单不存在");
        }

        GroupBuyOrder order = groupBuyOrderMapper.findByIdAndUserId(orderId, userId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!Integer.valueOf(ORDER_STATUS_UNPAID).equals(order.getStatus())) {
            throw new RuntimeException("当前订单不可支付");
        }
        if (isPaymentExpired(order.getCreateTime())) {
            cancelUnpaidOrder(userId, orderId);
            throw new RuntimeException("订单已超时，请重新下单");
        }

        GroupBuyTeam team = groupBuyTeamMapper.findByIdForUpdate(order.getTeamId());
        if (team == null) {
            throw new RuntimeException("拼团不存在");
        }
        if (!Integer.valueOf(TEAM_STATUS_ONGOING).equals(team.getStatus())) {
            throw new RuntimeException("该拼团已结束");
        }
        if (team.getExpireTime() != null && LocalDateTime.now().isAfter(team.getExpireTime())) {
            throw new RuntimeException("该拼团已超时");
        }
        if (team.getRequiredSize() != null && team.getPaidSize() != null
                && team.getPaidSize() >= team.getRequiredSize()) {
            throw new RuntimeException("该拼团已成团");
        }

        GroupBuyMember member = groupBuyMemberMapper.findByTeamIdAndUserId(team.getId(), userId);
        if (member == null) {
            throw new RuntimeException("拼团成员不存在");
        }

        GroupBuyActivity activity = groupBuyActivityMapper.findById(team.getActivityId());
        if (activity == null || !Integer.valueOf(ACTIVITY_STATUS_ENABLED).equals(activity.getStatus())) {
            throw new RuntimeException("拼团活动不存在或已下架");
        }

        Integer orderQuantity = order.getQuantity();
        int quantity = orderQuantity == null || orderQuantity <= 0 ? 1 : orderQuantity;
        int stockRows = groupBuyActivityMapper.decreaseStock(activity.getId(), quantity);
        if (stockRows == 0) {
            throw new RuntimeException("拼团库存不足");
        }

        int orderRows = groupBuyOrderMapper.updateStatusById(orderId, ORDER_STATUS_PAID_WAITING);
        if (orderRows == 0) {
            throw new RuntimeException("支付失败");
        }
        int memberRows = groupBuyMemberMapper.updatePayTime(member.getId());
        if (memberRows == 0) {
            throw new RuntimeException("支付失败");
        }
        int teamRows = groupBuyTeamMapper.increasePaidSize(team.getId());
        if (teamRows == 0) {
            throw new RuntimeException("该拼团已成团");
        }

        GroupBuyTeam updatedTeam = groupBuyTeamMapper.findById(team.getId());
        if (updatedTeam != null
                && updatedTeam.getRequiredSize() != null
                && updatedTeam.getPaidSize() != null
                && updatedTeam.getPaidSize() >= updatedTeam.getRequiredSize()) {
            groupBuyTeamMapper.markSuccess(updatedTeam.getId(), LocalDateTime.now());
            List<GroupBuyMember> members = groupBuyMemberMapper.listByTeamId(updatedTeam.getId());

            for (GroupBuyMember teamMember : members) {
                GroupBuyOrder teamOrder = groupBuyOrderMapper.findById(teamMember.getOrderId());
                if (teamOrder == null) {
                    continue;
                }
                if (Integer.valueOf(MEMBER_STATUS_PAID).equals(teamMember.getStatus())) {
                    groupBuyOrderMapper.updateStatusById(teamOrder.getId(), ORDER_STATUS_SUCCESS);
                } else {
                    groupBuyOrderMapper.updateStatusById(teamOrder.getId(), ORDER_STATUS_CANCELLED);
                    groupBuyMemberMapper.updateStatus(teamMember.getId(), MEMBER_STATUS_CANCELLED);
                }
            }
        }
    }

    @Override
    public GroupBuyTeamVO getTeamDetail(Long teamId) {
        GroupBuyTeam team = groupBuyTeamMapper.findById(teamId);
        if (team == null) {
            return null;
        }
        return toTeamVO(team);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelUnpaidOrder(Long userId, Long orderId) {
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        if (orderId == null) {
            throw new RuntimeException("订单不存在");
        }

        GroupBuyOrder order = groupBuyOrderMapper.findByIdAndUserId(orderId, userId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!Integer.valueOf(ORDER_STATUS_UNPAID).equals(order.getStatus())) {
            return;
        }

        GroupBuyMember member = groupBuyMemberMapper.findByTeamIdAndUserId(order.getTeamId(), userId);
        if (member != null) {
            groupBuyMemberMapper.updateStatus(member.getId(), MEMBER_STATUS_CANCELLED);
        }
        groupBuyOrderMapper.updateStatusById(orderId, ORDER_STATUS_CANCELLED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void expireTeam(Long teamId) {
        if (teamId == null) {
            throw new RuntimeException("拼团不存在");
        }

        GroupBuyTeam team = groupBuyTeamMapper.findByIdForUpdate(teamId);
        if (team == null) {
            return;
        }
        if (!Integer.valueOf(TEAM_STATUS_ONGOING).equals(team.getStatus())) {
            return;
        }
        if (team.getExpireTime() != null && LocalDateTime.now().isBefore(team.getExpireTime())) {
            return;
        }

        groupBuyTeamMapper.updateStatus(team.getId(), TEAM_STATUS_FAILED);
        List<GroupBuyMember> members = groupBuyMemberMapper.listByTeamId(team.getId());
        for (GroupBuyMember member : members) {
            GroupBuyOrder order = groupBuyOrderMapper.findById(member.getOrderId());
            if (order == null) {
                continue;
            }
            if (Integer.valueOf(MEMBER_STATUS_PAID).equals(member.getStatus())) {
                Integer orderQuantity = order.getQuantity();
                int quantity = orderQuantity == null || orderQuantity <= 0 ? 1 : orderQuantity;
                groupBuyActivityMapper.increaseStock(team.getActivityId(), quantity);
                groupBuyOrderMapper.updateStatusById(order.getId(), ORDER_STATUS_REFUNDED);
                groupBuyMemberMapper.updateStatus(member.getId(), MEMBER_STATUS_REFUNDED);
            } else {
                groupBuyOrderMapper.updateStatusById(order.getId(), ORDER_STATUS_CANCELLED);
                groupBuyMemberMapper.updateStatus(member.getId(), MEMBER_STATUS_CANCELLED);
            }
        }
    }

    @Override
    public List<GroupBuyOrderVO> listOrdersByUserId(Long userId) {
        return groupBuyOrderMapper.listByUserId(userId)
                .stream()
                .map(this::toOrderVO)
                .collect(Collectors.toList());
    }

    //管理端
    @Override
    public PageResult<GroupBuyActivityAdminVO> adminList(String keyword, Long productId, Integer status, Integer page, Integer size) {

        int currentPage = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 10 : size;
        int offset = (currentPage - 1) * pageSize;

        String realKeyword = null;
        if (keyword != null && !keyword.trim().isEmpty()) {
            realKeyword = keyword.trim();
        }

        long total = groupBuyActivityMapper.countAdminSearch(realKeyword, productId, status);
        List<GroupBuyActivityAdminVO> list = groupBuyActivityMapper.adminSearch(realKeyword, productId, status, offset, pageSize);

        log.info("接收到了");
        return PageResult.of(list, total, currentPage, pageSize);
    }

    @Override
    public GroupBuyActivityVO getActivityById(Long id) {
        GroupBuyActivity activity = groupBuyActivityMapper.findById(id);
        if (activity == null) {
            throw new RuntimeException("拼团活动不存在");
        }
        return toActivityVO(activity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addActivity(GroupBuyActivityRequestDTO request) {
        validateActivityRequest(request, false);

        Product product = productMapper.findById(request.getProductId());
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new RuntimeException("商品已下架");
        }
        if (product.getIsFlashSale() != null && product.getIsFlashSale() == 1) {
            throw new RuntimeException("秒杀商品暂不支持拼团");
        }

        GroupBuyActivity exist = groupBuyActivityMapper.findActiveByProductId(request.getProductId());
        if (exist != null) {
            throw new RuntimeException("该商品已有进行中的拼团活动");
        }

        if (request.getGroupPrice().compareTo(BigDecimal.valueOf(product.getPrice())) >= 0) {
            throw new RuntimeException("拼团价必须低于商品原价");
        }

        LocalDateTime now = LocalDateTime.now();
        GroupBuyActivity activity = new GroupBuyActivity();
        activity.setProductId(request.getProductId());
        activity.setGroupPrice(request.getGroupPrice());
        activity.setRequiredSize(request.getRequiredSize());
        activity.setDurationHours(request.getDurationHours());
        activity.setStock(request.getStock());
        activity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setCreateTime(now);
        activity.setUpdateTime(now);

        int rows = groupBuyActivityMapper.insert(activity);
        if (rows == 0) {
            throw new RuntimeException("新增失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateActivity(GroupBuyActivityRequestDTO request) {
        validateActivityRequest(request, true);

        GroupBuyActivity exist = groupBuyActivityMapper.findById(request.getId());
        if (exist == null) {
            throw new RuntimeException("拼团活动不存在");
        }

        Product product = productMapper.findById(request.getProductId());
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (product.getStatus() == null || product.getStatus() != 1) {
            throw new RuntimeException("商品已下架");
        }
        if (product.getIsFlashSale() != null && product.getIsFlashSale() == 1) {
            throw new RuntimeException("秒杀商品暂不支持拼团");
        }

        if (request.getGroupPrice().compareTo(BigDecimal.valueOf(product.getPrice())) >= 0) {
            throw new RuntimeException("拼团价必须低于商品原价");
        }

        exist.setProductId(request.getProductId());
        exist.setGroupPrice(request.getGroupPrice());
        exist.setRequiredSize(request.getRequiredSize());
        exist.setDurationHours(request.getDurationHours());
        exist.setStock(request.getStock());
        exist.setStatus(request.getStatus() == null ? exist.getStatus() : request.getStatus());
        exist.setStartTime(request.getStartTime());
        exist.setEndTime(request.getEndTime());
        exist.setUpdateTime(LocalDateTime.now());

        int rows = groupBuyActivityMapper.update(exist);
        if (rows == 0) {
            throw new RuntimeException("更新失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableActivity(Long id) {
        GroupBuyActivity activity = groupBuyActivityMapper.findById(id);
        if (activity == null) {
            throw new RuntimeException("拼团活动不存在");
        }
        activity.setStatus(1);
        activity.setUpdateTime(LocalDateTime.now());
        int rows = groupBuyActivityMapper.update(activity);
        if (rows == 0) {
            throw new RuntimeException("上架失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableActivity(Long id) {
        GroupBuyActivity activity = groupBuyActivityMapper.findById(id);
        if (activity == null) {
            throw new RuntimeException("拼团活动不存在");
        }
        activity.setStatus(0);
        activity.setUpdateTime(LocalDateTime.now());
        int rows = groupBuyActivityMapper.update(activity);
        if (rows == 0) {
            throw new RuntimeException("下架失败");
        }
    }


    //


    private void validateActivityRequest(GroupBuyActivityRequestDTO request, boolean requireId) {
        if (requireId && request.getId() == null) {
            throw new RuntimeException("活动ID不能为空");
        }
        if (request.getProductId() == null) {
            throw new RuntimeException("请选择商品");
        }
        if (request.getGroupPrice() == null || request.getGroupPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("拼团价必须大于 0");
        }
        if (request.getRequiredSize() == null || request.getRequiredSize() < 2) {
            throw new RuntimeException("成团人数至少为 2");
        }
        if (request.getDurationHours() == null || request.getDurationHours() <= 0) {
            throw new RuntimeException("活动时长必须大于 0");
        }
        if (request.getStock() == null || request.getStock() <= 0) {
            throw new RuntimeException("活动库存必须大于 0");
        }
    }
    private GroupBuyActivityAdminVO toAdminVO(GroupBuyActivity activity) {
        GroupBuyActivityAdminVO vo = new GroupBuyActivityAdminVO();
        vo.setId(activity.getId());
        vo.setProductId(activity.getProductId());
        vo.setGroupPrice(activity.getGroupPrice());
        vo.setRequiredSize(activity.getRequiredSize());
        vo.setDurationHours(activity.getDurationHours());
        vo.setStock(activity.getStock());
        vo.setStatus(activity.getStatus());
        vo.setStatusText(activity.getStatus() != null && activity.getStatus() == ACTIVITY_STATUS_ENABLED ? "进行中" : "已下架");
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setCreateTime(activity.getCreateTime());
        vo.setUpdateTime(activity.getUpdateTime());

        Product product = productMapper.findById(activity.getProductId());
        if (product != null) {
            vo.setProductPrice(BigDecimal.valueOf(product.getPrice()));
            vo.setProductName(product.getName());
        }
        return vo;
    }

    private GroupBuyActivityVO toActivityVO(GroupBuyActivity activity) {
        GroupBuyActivityVO vo = new GroupBuyActivityVO();
        vo.setId(activity.getId());
        vo.setProductId(activity.getProductId());
        vo.setGroupPrice(activity.getGroupPrice());
        vo.setRequiredSize(activity.getRequiredSize());
        vo.setDurationHours(activity.getDurationHours());
        vo.setStock(activity.getStock());
        vo.setStatus(activity.getStatus());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setStatusText(activity.getStatus() != null && activity.getStatus() == ACTIVITY_STATUS_ENABLED ? "进行中" : "已下架");
        return vo;
    }

    private GroupBuyTeamVO toTeamVO(GroupBuyTeam team) {
        GroupBuyTeamVO vo = new GroupBuyTeamVO();
        vo.setId(team.getId());
        vo.setTeamNo(team.getTeamNo());
        vo.setActivityId(team.getActivityId());
        vo.setProductId(team.getProductId());
        vo.setLeaderUserId(team.getLeaderUserId());
        vo.setRequiredSize(team.getRequiredSize());
        vo.setPaidSize(team.getPaidSize());
        vo.setRemainSize(team.getRequiredSize() == null || team.getPaidSize() == null
                ? null
                : Math.max(team.getRequiredSize() - team.getPaidSize(), 0));
        vo.setStatus(team.getStatus());
        vo.setStatusText(team.getStatus() != null && team.getStatus() == TEAM_STATUS_ONGOING
                ? "拼团中"
                : team.getStatus() != null && team.getStatus() == TEAM_STATUS_SUCCESS
                ? "已成团"
                : "已失败");
        vo.setExpireTime(team.getExpireTime());
        if (team.getExpireTime() != null) {
            vo.setRemainSeconds(Math.max(Duration.between(LocalDateTime.now(), team.getExpireTime()).getSeconds(), 0));
        }
        return vo;
    }

    private GroupBuyOrderVO toOrderVO(GroupBuyOrder order) {
        GroupBuyOrderVO vo = new GroupBuyOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setTeamId(order.getTeamId());
        vo.setActivityId(order.getActivityId());
        vo.setProductId(order.getProductId());
        vo.setProductName(order.getProductName());
        vo.setProductImage(order.getProductImage());
        vo.setGroupPrice(order.getGroupPrice());
        vo.setQuantity(order.getQuantity());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusText(groupBuyStatusText(order.getStatus()));
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());
        vo.setOrderType(3);
        return vo;
    }

    private String groupBuyStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付待成团";
            case 2 -> "已成团待发货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            case 5 -> "未成团已退款";
            default -> "未知";
        };
    }

    private boolean isPaymentExpired(LocalDateTime createTime) {
        return createTime != null
                && createTime.isBefore(LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES));
    }

    private String generateTeamNo(Long userId) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "GBT" + time + userId + random;
    }

    private String generateOrderNo(Long userId) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "GBO" + time + userId + random;
    }
}
