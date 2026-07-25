package com.homework.web.app.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.*;
import com.homework.model.enums.*;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.dto.MembershipOrderCreateDTO;
import com.homework.web.app.dto.MembershipPaymentConfirmationDTO;
import com.homework.web.app.mapper.*;
import com.homework.web.app.service.MembershipAccessService;
import com.homework.web.app.service.MembershipAccessSnapshot;
import com.homework.web.app.service.MembershipService;
import com.homework.web.app.vo.*;

import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Premium 与 Premium Plus 双台账的购买、查询和支付发放流程。 */
@Service
@RequiredArgsConstructor
public class MembershipServiceImpl implements MembershipService {

    private static final String DEFAULT_CURRENCY = "CNY";
    private static final int DIFF_UPGRADE_DAYS_PER_MONTH = 31;
    private static final int MAX_DIFF_UPGRADE_MONTHS = 12;

    private final MembershipPlanMapper membershipPlanMapper;
    private final BaseVipRecordMapper baseVipRecordMapper;
    private final SvipRecordMapper svipRecordMapper;
    private final MembershipOrderMapper membershipOrderMapper;
    private final UserInfoMapper userInfoMapper;
    private final MembershipAccessService membershipAccessService;


    @Override
    public MembershipInfoVO getMembershipInfo() {
        Long userId = LoginUserHolder.getUserId();
        if (userId == null) {
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }
        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfo::getId, userId)
                .eq(UserInfo::getStatus, UserInfoStatus.ACTIVE);
        UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);
        if (userInfo == null) {
            throw new HomeworkException(ResultCodeEnum.DATA_ERROR);
        }
        MembershipInfoVO vo = new MembershipInfoVO();
        vo.setDisplayName(userInfo.getDisplayName());
        vo.setAvatarUrl(userInfo.getAvatar());

        MembershipAccessSnapshot membership = membershipAccessService.getAccess(userId);
        vo.setMemberStatus(membership.status());
        vo.setMembershipType(membership.membershipType());
        vo.setExpiredTime(membership.currentExpireTime());
        vo.setBaseFreezeExpireTime(membership.baseFreezeExpireTime());
        return vo;
    }

    /** 返回当前身份、两类全款套餐和当前能够购买的补差档位。 */
    @Override
    public MembershipDetailPageVO getMembershipDetailPage() {
        Long userId = LoginUserHolder.getUserId();
        if (userId == null) {
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }

        LocalDateTime now = LocalDateTime.now();
        BaseVipRecord baseVip = baseVipRecordMapper.selectOne(
                new LambdaQueryWrapper<BaseVipRecord>()
                        .eq(BaseVipRecord::getUserId, userId)
                        .last("LIMIT 1")
        );
        SvipRecord svip = svipRecordMapper.selectOne(
                new LambdaQueryWrapper<SvipRecord>()
                        .eq(SvipRecord::getUserId, userId) //userId设置唯一索引
                        .last("LIMIT 1")
        );

        //查询是否存在 有效的premium
        boolean svipActive = svip != null && svip.getExpireTime() != null && svip.getExpireTime().isAfter(now);
        //查询是否存在 有效的premium plus
        boolean baseVipActive = baseVip != null && baseVip.getExpireTime() != null && baseVip.getExpireTime().isAfter(now);

        //创建会员详情页对象
        MembershipDetailPageVO page = new MembershipDetailPageVO();
        if (svipActive) {
            page.setMemberStatus(MembershipStatus.PREMIUM_PLUS);
            page.setCurrentMembershipType(MembershipType.PREMIUM_PLUS);
            page.setCurrentExpireTime(svip.getExpireTime());

            //同时存在有效的premium 和 premium plus, 优先使用 premium plus 并 冻结 premium
            if (baseVipActive && baseVip.getExpireTime().isAfter(svip.getExpireTime())) {
                page.setBaseFreezeExpireTime(baseVip.getExpireTime());
            }
        } else if (baseVipActive) { //如果只有premium, 那就返回premium的信息，freezeExpireTime = null
            page.setMemberStatus(MembershipStatus.PREMIUM);
            page.setCurrentMembershipType(MembershipType.PREMIUM);
            page.setCurrentExpireTime(baseVip.getExpireTime());
        } else {
            page.setMemberStatus(MembershipStatus.FREE);
        }

        //剩余premium的计算开始时间
        LocalDateTime remainingBaseVipStart = svipActive ? svip.getExpireTime() : now;

        //用户最多可以补差额升级多少个月的 premium plus，默认是0
        int maxDiffUpgradeMonths = 0;
        if (baseVip != null && baseVip.getExpireTime() != null && baseVip.getExpireTime().isAfter(remainingBaseVipStart)) {
            //剩余天数的计算（转换成Day）
            long remainingDays = Duration.between(remainingBaseVipStart, baseVip.getExpireTime()).toDays();

            //该用户实际可以补差额升级到 premium plus 的最大月份数
            //这里执行的是整数除法，小数部分会被直接舍弃
            //因此当 29.2 / 31，那么结果就是0
            maxDiffUpgradeMonths = (int) Math.min(MAX_DIFF_UPGRADE_MONTHS, remainingDays / DIFF_UPGRADE_DAYS_PER_MONTH);
        }

        //查询所有 有效的 会员菜单
        List<MembershipPlan> availablePlans = membershipPlanMapper.selectList(
                new LambdaQueryWrapper<MembershipPlan>()
                        .eq(MembershipPlan::getEnabled, Boolean.TRUE)
                        .orderByAsc(MembershipPlan::getPurchaseType)
                        .orderByAsc(MembershipPlan::getMembershipType)
                        .orderByAsc(MembershipPlan::getDurationMonths)
        );

        //创建 全款premium的系列菜单
        List<MembershipSkuVO> premiumFullPurchaseOptions = new ArrayList<>();
        //创建 全款premiumPlus的系列菜单
        List<MembershipSkuVO> premiumPlusFullPurchaseOptions = new ArrayList<>();
        //创建 补差的菜单
        List<MembershipSkuVO> diffUpgradeOptions = new ArrayList<>();

        for (MembershipPlan plan : availablePlans) {

            //不论哪个菜单，都共用同一个 菜单模板，这样就可以遍历 菜单数据库，获得每一个planId的行数据，自动对应上 premium/premium plus/补差 菜单
            MembershipSkuVO sku = new MembershipSkuVO();
            sku.setPlanId(plan.getId());
            sku.setPurchaseType(plan.getPurchaseType());
            sku.setBillingType(plan.getBillingType());
            sku.setDurationMonths(plan.getDurationMonths()); //如果是全款，这里是1/3/12；如果是补差，这里是1/2/3/4/5/6/7/8/9/10/11
            sku.setPrice(plan.getPrice().setScale(0, RoundingMode.HALF_UP));
            sku.setCurrency(StringUtils.hasText(plan.getCurrency()) ? plan.getCurrency() : DEFAULT_CURRENCY);

            //每set完一个sku, 就把它放到对应的sku列表中
            //因为是 one by one 的放，所以根据if判断，可以实现控制存入到哪一个sku列表的目的
            if (plan.getPurchaseType() == MembershipPurchaseType.FULL && plan.getMembershipType() == MembershipType.PREMIUM) {
                premiumFullPurchaseOptions.add(sku);
            } else if (plan.getPurchaseType() == MembershipPurchaseType.FULL && plan.getMembershipType() == MembershipType.PREMIUM_PLUS) {
                premiumPlusFullPurchaseOptions.add(sku);
            } else if (plan.getPurchaseType() == MembershipPurchaseType.DIFF && plan.getDurationMonths() <= maxDiffUpgradeMonths) {
                diffUpgradeOptions.add(sku); //diffUpgradeOptions 是有可能返回 空列表的，因为 maxDiffUpgradeMonths 可以等于0，那么就是 不满足补差条件
            }
        }

        //装配全款 premium 的 sku列表
        MembershipPlanCardVO premiumCard = new MembershipPlanCardVO();
        premiumCard.setMembershipType(MembershipType.PREMIUM);
        premiumCard.setFullPurchaseOptions(premiumFullPurchaseOptions);
        //装配全款 premium plus的 sku列表
        MembershipPlanCardVO premiumPlusCard = new MembershipPlanCardVO();
        premiumPlusCard.setMembershipType(MembershipType.PREMIUM_PLUS);
        premiumPlusCard.setFullPurchaseOptions(premiumPlusFullPurchaseOptions);

        //装配会员详情页
        page.setFullPurchaseCards(List.of(premiumCard, premiumPlusCard));
        page.setDiffUpgradeAvailable(maxDiffUpgradeMonths > 0); //告诉前端，是否展示补差模块
        page.setMaxDiffUpgradeMonths(maxDiffUpgradeMonths);
        page.setDiffUpgradeOptions(diffUpgradeOptions);
        return page;
    }

    /** 校验套餐和补差资格后创建一笔等待微信支付的订单。 */
    @Transactional
    @Override
    public MembershipOrderCreateVO createOrder(String idempotencyKey, MembershipOrderCreateDTO dto //idempotencyKey 防重复建单凭证
    ) {
        Long userId = LoginUserHolder.getUserId();
        if (userId == null) {
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }
        if (!StringUtils.hasText(idempotencyKey)
                || idempotencyKey.length() > 64
                || dto == null
                || dto.getPlanId() == null) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        //创建用户锁，防止把同一用户的创建订单操作串行化
        UserInfo lockedUser = userInfoMapper.selectOne(
                new LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getId, userId)
                        .last("LIMIT 1 FOR UPDATE")
        );

        if (lockedUser == null) {
            throw new HomeworkException(
                    ResultCodeEnum.APP_LOGIN_USER_NOT_EXIST
            );
        }

        //这一步是在查，用户是否有并发操作，或者网络重试
        //核心宗旨是在查，有没有两个相同的订单
        MembershipOrder existing = membershipOrderMapper.selectOne(
                new LambdaQueryWrapper<MembershipOrder>()
                        .eq(MembershipOrder::getUserId, userId) // userId + idempotencyKey = 唯一索引
                        .eq(MembershipOrder::getIdempotencyKey, idempotencyKey) //idempotencyKey 用于防止用户因为重复点击或网络重试而创建多笔相同订单。
                        .last("LIMIT 1")
        );
        if (existing != null) { //如果这笔订单已经有了，直接返回，不用重复创建了
            MembershipOrderCreateVO result = new MembershipOrderCreateVO();
            result.setOrderNo(existing.getOrderNo());
            result.setOrderStatus(existing.getOrderStatus());
            result.setAmountDue(existing.getPayAmount());
            result.setCurrency(existing.getCurrency());
            result.setPaymentExpiredTime(existing.getPaymentExpiredTime());
            result.setCodeUrl(existing.getPaymentCodeUrl());
            return result;
        }

        //如果没有两个相同的订单
        //开始创建这笔订单
        //now 是 本次订单的 创建时间
        LocalDateTime now = LocalDateTime.now();

        //查看 是否有pending的订单，并且已经过了支付期限了
        List<MembershipOrder> expiredOrders = membershipOrderMapper.selectList(
                new LambdaQueryWrapper<MembershipOrder>()
                        .eq(MembershipOrder::getUserId, userId)
                        .eq(MembershipOrder::getOrderStatus, MembershipOrderStatus.PENDING)
                        .le(MembershipOrder::getPaymentExpiredTime, now)
        );
        //如果有，遍历这些订单，把每一个订单状态都改为Expired
        for (MembershipOrder expiredOrder : expiredOrders) {
            expiredOrder.setOrderStatus(MembershipOrderStatus.EXPIRED);
            membershipOrderMapper.updateById(expiredOrder);
        }
        //继续计算 再检查用户是否还有尚未超时的待支付订单
        Long pendingCount = membershipOrderMapper.selectCount(
                new LambdaQueryWrapper<MembershipOrder>()
                        .eq(MembershipOrder::getUserId, userId)
                        .eq(MembershipOrder::getOrderStatus, MembershipOrderStatus.PENDING)
        );
        if (pendingCount > 0) { //如果还有，说明当前有尚未支付的订单（同一个用户同一时间只能存在一笔尚未完成的会员订单）
            /*
            如果允许同时存在多个待支付订单，会出现：
            页面同时存在多个微信二维码；
            用户可能意外支付多笔订单；
            补差订单创建后，基础会员剩余时长可能发生变化；
            多笔支付回调可能先后修改同一组会员台账；
            用户不知道应该继续支付哪一笔订单。
             */
            throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_CHANGE_IN_PROGRESS);
        }

        //检查要支付的plan item还在不在
        MembershipPlan plan = membershipPlanMapper.selectOne(
                new LambdaQueryWrapper<MembershipPlan>()
                        .eq(MembershipPlan::getId, dto.getPlanId())
                        .eq(MembershipPlan::getEnabled, Boolean.TRUE)
                        .last("LIMIT 1")
        );
        if (plan == null || plan.getPurchaseType() == null || plan.getMembershipType() == null || plan.getDurationMonths() == null || plan.getPrice() == null
                || plan.getPrice().signum() <= 0) {
            throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_PLAN_NOT_FOUND);
        }

        //表示 这笔已经创建的订单，在微信支付成功后，应当执行哪一种会员变更动作。
        //先设置一个默认值：全款买
        MembershipOrderAction action = MembershipOrderAction.FULL_PURCHASE;

        //如果用户点击传入的购买指令是 补差plan中的一种
        if (plan.getPurchaseType() == MembershipPurchaseType.DIFF) {
            //以下情况都是不能购买补差套餐的
            if (plan.getMembershipType() != MembershipType.PREMIUM_PLUS || plan.getDurationMonths() < 1 || plan.getDurationMonths() > MAX_DIFF_UPGRADE_MONTHS) {
                throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_PLAN_NOT_FOUND);
            }

            //这个BaseVipRecord 和 SvipRecord，实际上就是用户的“两张会员卡”
            //它们不会变，只是在不断更新上面的到期日期
            /*
            查询当前会员状态只需要读一条记录；
            续费直接延长一个 expireTime；
            Premium Plus 冻结时只需要移动一个最终到期时间；
            补差资格只需要计算一个剩余时长；
            支付回调可以锁定并更新唯一一条用户台账。
             */
            BaseVipRecord baseVip = baseVipRecordMapper.selectOne(
                    new LambdaQueryWrapper<BaseVipRecord>()
                            .eq(BaseVipRecord::getUserId, userId) //设置 userId 唯一，是为了保证一个用户只有一个可计算的基础VIP账户
                            .last("LIMIT 1")
            );
            if (baseVip == null || baseVip.getExpireTime() == null) {
                throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_DIFF_UPGRADE_UNAVAILABLE); //不具备补差资格
            }
            SvipRecord svip = svipRecordMapper.selectOne(
                    new LambdaQueryWrapper<SvipRecord>()
                            .eq(SvipRecord::getUserId, userId) //设置 userId 唯一，是为了保证一个用户只有一个可计算的SVIP账户：
                            .last("LIMIT 1")
            );

            //用户的 premium 剩余天数转换的 月份数的整数 是否大于等于 plan所选的 durationMonths
            //premium 的起算时间，要以 plus到期时间 或者 创建订单时间 now
            //为什么要先查plus，因为如果有plus, premium的开始时间一定是在 plus 的截止时间
            LocalDateTime baseVipStartTime = svip != null && svip.getExpireTime().isAfter(now) ? svip.getExpireTime() : now;
            long remainingDays = Duration.between(baseVipStartTime, baseVip.getExpireTime()).toDays();

            int maxDiffUpgradeMonths = (int)Math.min(MAX_DIFF_UPGRADE_MONTHS, (remainingDays / DIFF_UPGRADE_DAYS_PER_MONTH));

            //如果用户选择的 durationMonths 大于可补差的 最大可补差月数，就抛出异常
            //比如（最多只能补3个月，但是你选的plan.getDurationMonths = 4)
            if(plan.getDurationMonths() > maxDiffUpgradeMonths){
                throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_DIFF_UPGRADE_UNAVAILABLE);
            }
            //如果等于，就把action设置为 补差
            action = MembershipOrderAction.DIFF_UPGRADE;
        }

        MembershipOrder order = new MembershipOrder();
        order.setUserId(userId);
        order.setOrderNo(IdUtil.getSnowflakeNextIdStr());
        order.setAction(action);
        order.setToPlanId(plan.getId());
        order.setMembershipType(plan.getMembershipType());
        order.setBillingType(plan.getBillingType());
        order.setDurationMonths(plan.getDurationMonths());
        order.setPayAmount(plan.getPrice().setScale(2, RoundingMode.HALF_UP));
        order.setCurrency(StringUtils.hasText(plan.getCurrency()) ? plan.getCurrency() : DEFAULT_CURRENCY);
        order.setOrderStatus(MembershipOrderStatus.PENDING);
        order.setIdempotencyKey(idempotencyKey);
        order.setPaymentExpiredTime(now.plusMinutes(15));
        membershipOrderMapper.insert(order);

        MembershipOrderCreateVO result = new MembershipOrderCreateVO();
        result.setOrderNo(order.getOrderNo());
        result.setOrderStatus(order.getOrderStatus());
        result.setAmountDue(order.getPayAmount());
        result.setCurrency(order.getCurrency());
        result.setPaymentExpiredTime(order.getPaymentExpiredTime());
        return result;
    }

    @Override
    public List<MembershipOrderHistoryVO> getOrderHistory() {
        Long userId = LoginUserHolder.getUserId();
        if (userId == null) {
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }

        List<MembershipOrder> orders = membershipOrderMapper.selectList(
                new LambdaQueryWrapper<MembershipOrder>()
                        .eq(MembershipOrder::getUserId, userId)
                        .orderByDesc(MembershipOrder::getId)
        );
        List<MembershipOrderHistoryVO> result = new ArrayList<>(orders.size());
        for (MembershipOrder order : orders) {
            MembershipOrderHistoryVO item = new MembershipOrderHistoryVO();
            item.setOrderNo(order.getOrderNo());
            item.setAction(order.getAction());
            item.setMembershipType(order.getMembershipType());
            item.setBillingType(order.getBillingType());
            item.setDurationMonths(order.getDurationMonths());
            item.setPayAmount(order.getPayAmount());
            item.setCurrency(order.getCurrency());
            item.setOrderStatus(order.getOrderStatus());
            item.setPeriodEnd(order.getPeriodEnd());
            item.setPayTime(order.getPayTime());
            result.add(item);
        }
        return result;
    }

    @Override
    public MembershipOrderStatus getOrderStatus(String orderNo) {
        Long userId = LoginUserHolder.getUserId();
        if (userId == null) {
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }
        if (!StringUtils.hasText(orderNo)) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        MembershipOrder order = membershipOrderMapper.selectOne(
                new LambdaQueryWrapper<MembershipOrder>()
                        .eq(MembershipOrder::getOrderNo, orderNo)
                        .eq(MembershipOrder::getUserId, userId)
                        .last("LIMIT 1")
        );
        if (order == null) {
            throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_ORDER_NOT_FOUND);
        }
        return order.getOrderStatus();
    }

    /**
     * 处理已经通过微信验签的支付成功结果，并正式发放会员权益。
     *
     * <p>这里的 confirmation 不是前端传入的数据，而是
     * WechatNativePaymentGateway 对微信回调完成验签、解密和内容检查后生成的可信支付结果。</p>
     *
     * <p>整个方法在同一个事务中完成以下事情：</p>
     * <ol>
     *     <li>锁定会员订单，防止微信重复回调被同时处理；</li>
     *     <li>核对支付时间、金额和币种；</li>
     *     <li>锁定用户的 Premium 和 Premium Plus 台账；</li>
     *     <li>根据订单的 action 和 membershipType 计算并更新会员到期时间；</li>
     *     <li>最后把订单更新为已支付。</li>
     * </ol>
     *
     * @param confirmation 微信支付回调验签通过后生成的支付确认信息
     */
    @Transactional
    @Override
    public void confirmPayment(MembershipPaymentConfirmationDTO confirmation) {
        // 第一步：检查支付确认信息是否完整。这里不再负责微信验签，验签已经由 Gateway 完成。
        if (confirmation == null
                || !StringUtils.hasText(confirmation.getOrderNo())
                || !StringUtils.hasText(confirmation.getProviderTradeNo())
                || confirmation.getPaidAmount() == null
                || !StringUtils.hasText(confirmation.getCurrency())) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        // 第二步：根据商户订单号查询并锁定订单。
        // FOR UPDATE 会让同一订单的其他支付回调等待当前事务结束，避免重复发放会员权益。
        MembershipOrder order = membershipOrderMapper.selectOne(
                new LambdaQueryWrapper<MembershipOrder>()
                        .eq(MembershipOrder::getOrderNo, confirmation.getOrderNo())
                        .last("LIMIT 1 FOR UPDATE")
        );
        if (order == null) {
            throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_ORDER_NOT_FOUND);
        }
        // 微信可能重复发送同一个支付通知。订单已经支付过时直接返回，不能再次增加会员时间。
        if (order.getOrderStatus() == MembershipOrderStatus.PAID) {
            return;
        }

        // 第三步：设置支付时间，检查用户是否在订单有效期内完成了付款。
        LocalDateTime paidTime = confirmation.getPaidTime() == null ? LocalDateTime.now() : confirmation.getPaidTime();
        if (order.getPaymentExpiredTime() == null || paidTime.isAfter(order.getPaymentExpiredTime())) { //没有设置订单支付时限 或 支付时间超过时效，都不行
            throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_ORDER_STATE_ERROR);
        }
        // 再核对本地订单金额、币种与微信实际收款结果，防止错误金额的订单获得会员权益。
        if (order.getPayAmount().setScale(2, RoundingMode.HALF_UP)
                .compareTo(confirmation.getPaidAmount().setScale(2, RoundingMode.HALF_UP)) != 0
                || !order.getCurrency().equalsIgnoreCase(confirmation.getCurrency())) {
            throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_PAYMENT_MISMATCH);
        }

        // 第四步：锁定用户唯一的两张会员台账。
        // 后面必须基于锁定后的最新到期时间计算，避免两个支付回调同时覆盖彼此的结果。
        BaseVipRecord baseVip = baseVipRecordMapper.selectOne(
                new LambdaQueryWrapper<BaseVipRecord>()
                        .eq(BaseVipRecord::getUserId, order.getUserId())
                        .last("LIMIT 1 FOR UPDATE")
        );
        SvipRecord svip = svipRecordMapper.selectOne(
                new LambdaQueryWrapper<SvipRecord>()
                        .eq(SvipRecord::getUserId, order.getUserId())
                        .last("LIMIT 1 FOR UPDATE")
        );

        //定义一个新变量：新增订单的开始时间
        LocalDateTime newSvipExtensionStartTime;
        LocalDateTime newBaseVipExtensionStartTime;

        // 再定义一个本次新增订单会员时长的到期时间
        LocalDateTime newSvipExtensionEndTime;
        LocalDateTime newBaseVipExtensionEndTime;
        
        // 分支一：全款购买 Premium。
        // 这个部分不涉及到对 Premium plus的任何操作，只是查询
        if (order.getAction() == MembershipOrderAction.FULL_PURCHASE && order.getMembershipType() == MembershipType.PREMIUM) {

            //用户是想购买premium，一级分类就是premium
            if (baseVip == null) {
                baseVip = new BaseVipRecord();
                baseVip.setUserId(order.getUserId());

                //检查有没有 有效的premium plus，有的话，那么premium的起算时间要从plus结束时开始计算
                newBaseVipExtensionStartTime = svip != null && svip.getExpireTime().isAfter(paidTime) ? svip.getExpireTime() : paidTime;
                newBaseVipExtensionEndTime = newBaseVipExtensionStartTime.plusMonths(order.getDurationMonths());
                baseVip.setExpireTime(newBaseVipExtensionEndTime);
                baseVipRecordMapper.insert(baseVip);
            } else if(baseVip.getExpireTime().isAfter(paidTime)) { //如果用户有 有效的premium会员，就把新买的premium时长加到原有的截止时间
                newBaseVipExtensionStartTime = baseVip.getExpireTime();
                newBaseVipExtensionEndTime = newBaseVipExtensionStartTime.plusMonths(order.getDurationMonths());
                baseVip.setExpireTime(newBaseVipExtensionEndTime);
                baseVipRecordMapper.updateById(baseVip);
            }else { //有会员但已过期,那么既然买的就是premium，就把起算时间设为支付时间
                newBaseVipExtensionStartTime = svip != null && svip.getExpireTime().isAfter(paidTime) ? svip.getExpireTime() : paidTime;
                newBaseVipExtensionEndTime = newBaseVipExtensionStartTime.plusMonths(order.getDurationMonths());
                baseVip.setExpireTime(newBaseVipExtensionEndTime);
                baseVipRecordMapper.updateById(baseVip);
            }
            order.setPeriodEnd(newBaseVipExtensionEndTime);
        // 分支二：全款购买 Premium Plus。
        } else if (order.getAction() == MembershipOrderAction.FULL_PURCHASE && order.getMembershipType() == MembershipType.PREMIUM_PLUS) {

            //用户是想购买premium plus，一级分类就是premium plus
            //没有premium plus, 立刻新增premium plus，时长从支付开始起算
            if (svip == null) {
                svip = new SvipRecord();
                svip.setUserId(order.getUserId());
                newSvipExtensionStartTime = paidTime;
                newSvipExtensionEndTime = newSvipExtensionStartTime.plusMonths(order.getDurationMonths());
                svip.setExpireTime(newSvipExtensionEndTime);
                svipRecordMapper.insert(svip);
                if(baseVip != null && baseVip.getExpireTime().isAfter(paidTime)) {
                    //先算一下增加了多少天的svip
                    int svipExtensionDays = order.getDurationMonths() * DIFF_UPGRADE_DAYS_PER_MONTH;
                    newBaseVipExtensionStartTime = svip.getExpireTime();
                    newBaseVipExtensionEndTime = newBaseVipExtensionStartTime.plusDays(svipExtensionDays);
                    baseVip.setExpireTime(newBaseVipExtensionEndTime);
                    baseVipRecordMapper.updateById(baseVip);
                }

                //存在有效premium plus，时长更新在原plus结尾
            } else if(svip.getExpireTime() != null && svip.getExpireTime().isAfter(paidTime)) {
                newSvipExtensionStartTime = svip.getExpireTime();
                newSvipExtensionEndTime = newSvipExtensionStartTime.plusMonths(order.getDurationMonths());
                svip.setExpireTime(newSvipExtensionEndTime);
                svipRecordMapper.updateById(svip);

                //如果原来有 有效的premium，顺延精确天数
                //为什么买的时候是自然月，但是顺延要精确到秒？
                //因为买到时候是接受 买到是自然月这个事实的，但是冻结过程中你不能随便减我的天数
                if(baseVip != null && baseVip.getExpireTime().isAfter(paidTime)) {
                    //Duration 可以精确到纳秒
                    Duration remainingBaseVipTime = Duration.between(newSvipExtensionStartTime, baseVip.getExpireTime());
                    newBaseVipExtensionStartTime = svip.getExpireTime();
                    newBaseVipExtensionEndTime = newBaseVipExtensionStartTime.plus(remainingBaseVipTime);
                    baseVip.setExpireTime(newBaseVipExtensionEndTime);
                    baseVipRecordMapper.updateById(baseVip);
                }
                
            }else { //plus过期，但有plus会员卡，时长从支付开始起算
                newSvipExtensionStartTime = paidTime;
                newSvipExtensionEndTime = newSvipExtensionStartTime.plusMonths(order.getDurationMonths());
                svip.setExpireTime(newSvipExtensionEndTime);
                svipRecordMapper.updateById(svip);
            }
            //如果用户存在 有效的premium

            order.setPeriodEnd(newSvipExtensionEndTime);

        // 分支三：补差购买 Premium Plus
        } else if (order.getAction() == MembershipOrderAction.DIFF_UPGRADE && order.getMembershipType() == MembershipType.PREMIUM_PLUS) {

            //其实这一步已经在前面 createOrder 里做了，不合格的选项，不会进入到回调环节的
            int svipExtensionDays = order.getDurationMonths() * DIFF_UPGRADE_DAYS_PER_MONTH;
            //如果用户存在 有效的premium
            //注意：这是补差，是时间转换，不是顺延
                if(svip != null && svip.getExpireTime().isAfter(paidTime)){
                    newSvipExtensionStartTime = svip.getExpireTime();
                    newSvipExtensionEndTime = newSvipExtensionStartTime.plusDays(svipExtensionDays);
                    svip.setExpireTime(newSvipExtensionEndTime);
                    svipRecordMapper.updateById(svip);
                }else if(svip == null){
                    svip = new SvipRecord();
                    svip.setUserId(order.getUserId());
                    newSvipExtensionStartTime = paidTime;
                    newSvipExtensionEndTime = newSvipExtensionStartTime.plusDays(svipExtensionDays);
                    svip.setExpireTime(newSvipExtensionEndTime);
                    svipRecordMapper.insert(svip);
                }else {
                    newSvipExtensionStartTime = paidTime;
                    newSvipExtensionEndTime = newSvipExtensionStartTime.plusDays(svipExtensionDays);
                    svip.setExpireTime(newSvipExtensionEndTime);
                    svipRecordMapper.updateById(svip);
                }


            order.setPeriodEnd(newSvipExtensionEndTime);
        } else {
            // 订单动作与目标会员类型不属于系统允许的组合，拒绝发放权益。
            throw new HomeworkException(ResultCodeEnum.MEMBERSHIP_ORDER_STATE_ERROR);
        }

        // 第五步：会员权益更新成功后，再保存微信交易号、支付时间、本次到期时间，并完成订单。
        // 以上任一步骤抛出异常时，@Transactional 会让本次会员和订单修改一起回滚。
        order.setProviderTradeNo(confirmation.getProviderTradeNo());
        order.setPayTime(paidTime);
        order.setOrderStatus(MembershipOrderStatus.PAID);
        membershipOrderMapper.updateById(order);
    }
}
