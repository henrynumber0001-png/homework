package com.homework.web.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.PageResult;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.AdminBankScope;
import com.homework.model.entity.BankTag;
import com.homework.model.entity.CategoryGroup;
import com.homework.model.entity.CategoryModule;
import com.homework.model.entity.CategorySubModule;
import com.homework.model.entity.CertificateQuestionInfo;
import com.homework.model.entity.InterviewQuestionInfo;
import com.homework.model.entity.QuestionBank;
import com.homework.model.enums.AdminRole;
import com.homework.model.enums.BankDataScope;
import com.homework.model.enums.GroupType;
import com.homework.model.enums.QuestionBankStatus;
import com.homework.web.admin.auth.AdminAccessService;
import com.homework.web.admin.context.AdminContext;
import com.homework.web.admin.dto.QuestionBankCreateDTO;
import com.homework.web.admin.dto.QuestionBankUpdateDTO;
import com.homework.web.admin.dto.ResourceActionDTO;
import com.homework.web.admin.mapper.AdminBankScopeMapper;
import com.homework.web.admin.mapper.BankTagMapper;
import com.homework.web.admin.mapper.CategoryGroupMapper;
import com.homework.web.admin.mapper.CategoryModuleMapper;
import com.homework.web.admin.mapper.CategorySubModuleMapper;
import com.homework.web.admin.mapper.CertificateQuestionMapper;
import com.homework.web.admin.mapper.InterviewQuestionMapper;
import com.homework.web.admin.mapper.QuestionBankMapper;
import com.homework.web.admin.vo.ActionResultVO;
import com.homework.web.admin.vo.QuestionBankDetailVO;
import com.homework.web.admin.vo.QuestionBankRowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/** 后台题库查询、创建、编辑和状态管理。 */
@Service
@RequiredArgsConstructor
public class AdminQuestionBankService {

    private final QuestionBankMapper bankMapper;
    private final CategorySubModuleMapper subModuleMapper;
    private final CategoryModuleMapper moduleMapper;
    private final CategoryGroupMapper groupMapper;
    private final InterviewQuestionMapper interviewQuestionMapper;
    private final CertificateQuestionMapper certificateQuestionMapper;
    private final BankTagMapper bankTagMapper;
    private final AdminBankScopeMapper bankScopeMapper;
    private final AdminAccessService accessService;
    private final QuestionBankAssembler assembler;
    private final AdminAuditService auditService;

    public PageResult<QuestionBankRowVO> list(
            String keyword, //关键词
            Long groupId,
            Long moduleId,
            Long subModuleId,
            QuestionBankStatus status,
            Integer pageNum,
            Integer pageSize,
            String sortMode
    ) {
        // 前端没传，默认页码为1。
        int selectPage = 1;
        if (pageNum != null && pageNum >= 1) {
            selectPage = pageNum; //前端传了，按前端的设置
        }

        // 默认每页20条，并限制在1到100之间。
        int defaultSize = 20;
        if (pageSize != null) {
            defaultSize = Math.min(Math.max(pageSize, 1), 100); //pageSize是前端传回来的，具体是否开放权限给用户选择，还是前端写死，要看你自己的业务逻辑
        }

        // 前端没传，默认值是：按更新时间降序
        String selectSortMode = "UPDATED_TIME_DESC";
        if (sortMode != null && !sortMode.isBlank()) {
            selectSortMode = sortMode.trim().toUpperCase(Locale.ROOT); //如果前端更新了，那么就按照前端传入的
        }
        if (!"UPDATED_TIME_DESC".equals(selectSortMode) && !"SORT_ORDER_DESC".equals(selectSortMode)) {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        List<Long> allowedBankIds = accessService.listAssignedBankIds(AdminContext.getAdminId());


        //是否提供全部的查询字段列表，还是锁住，这些是前端的功能
        //先选择groupId,然后moduleId变成可选项；当选择了moduleId后，submoduleId变成可选项。
        //要先预设值一个 既可以接收单一 subModuleId 也可以接收 subModuleIds 的 成员变量，因为不论管理员输入何种查询条件，最终要想到questionBank中查询对应的数据，都要通过 subModuleId
        List<Long> categorySubModuleIds = null;

        //在Admin侧的题库页面，只有四种选择：1.全不选；2.只选择groupId；3.选择groupId和moduleId；4.选择groupId、moduleId和subModuleId
        if (groupId != null && moduleId == null && subModuleId == null) {
            CategoryGroup group = groupMapper.selectById(groupId); //只有group还不够用，还需要继续向下查询到group对应的submoduleId, 因为question_bank里只有submoduleId
            if (group == null) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_CATEGORY_INVALID);
            }
            LambdaQueryWrapper<CategoryModule> moduleQuery = new LambdaQueryWrapper<>();
            moduleQuery.eq(CategoryModule::getGroupId, groupId);

            List<CategoryModule> modules = moduleMapper.selectList(moduleQuery);
            if(modules.isEmpty()) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_CATEGORY_INVALID);
            }
            List<Long> moduleIds = modules.stream().map(CategoryModule::getId).toList();

            LambdaQueryWrapper<CategorySubModule> subModuleQuery = new LambdaQueryWrapper<>();
            subModuleQuery.in(CategorySubModule::getModuleId, moduleIds);
            List<CategorySubModule> categorySubModules = subModuleMapper.selectList(subModuleQuery);
            if(categorySubModules.isEmpty()) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_CATEGORY_INVALID);
            }
            categorySubModuleIds = categorySubModules.stream().map(CategorySubModule::getId).toList();

        }
        if(groupId != null && moduleId != null && subModuleId == null) {
            CategoryModule module = moduleMapper.selectById(moduleId);
            if (module == null) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_CATEGORY_INVALID);
            }

            LambdaQueryWrapper<CategorySubModule> subModuleQuery = new LambdaQueryWrapper<>();
            subModuleQuery.eq(CategorySubModule::getModuleId,module.getId());
            List<CategorySubModule> subModules = subModuleMapper.selectList(subModuleQuery);
            if(subModules.isEmpty()) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_CATEGORY_INVALID);
            }
            categorySubModuleIds = subModules.stream().map(CategorySubModule::getId).toList();
        }
        if(groupId != null && moduleId != null && subModuleId != null) {
            CategorySubModule subModule = subModuleMapper.selectById(subModuleId);
            if (subModule == null) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_CATEGORY_INVALID);
            }
            categorySubModuleIds = List.of(subModule.getId());
        }


        // 非超级管理员且数据范围为 ASSIGNED_BANKS 时，只能查看明确分配给自己的题库。
        boolean assignedOnly = AdminContext.get().getRole() != AdminRole.SUPER_ADMIN && AdminContext.get().getBankDataScope() == BankDataScope.ASSIGNED_BANKS;

        // 管理员只能看已分配题库，但 allowedBankIds 为空，说明目前没有给他分配任何题库；
        // 此时要直接返回空分页，因为 allowedBankIds = [] → 无法安全生成 IN 条件 → 提前返回空分页
        if (assignedOnly && allowedBankIds.isEmpty()) {
            PageResult<QuestionBankRowVO> empty = new PageResult<>();
            empty.setRecords(List.of());
            empty.setTotal(0);
            empty.setPageNum(selectPage);
            empty.setPageSize(defaultSize);
            return empty;
        }

        // 创建未删除题库的分页查询条件；MyBatis-Plus 会自动追加 is_deleted = 0。
        LambdaQueryWrapper<QuestionBank> query = new LambdaQueryWrapper<>();
        // 只有关键词非空且不是纯空白时，才添加名称或 ID 查询条件。
        if (keyword != null && !keyword.isBlank()) {
            // 去掉关键词首尾空白，避免空格影响名称和 ID 匹配。
            String normalizedKeyword = keyword.trim();
            // 用括号包住“名称匹配 OR ID 匹配”，避免 OR 影响外层其他 AND 条件。
            query.and(wrapper -> {
                // 第一种关键词匹配方式：题库名称包含输入内容。
                wrapper.like(QuestionBank::getBankName, normalizedKeyword);
                // 尝试把关键词解析成 Long，以便同时支持按题库 ID 查询。
                try {
                    // 解析成功时追加 OR id = 关键词。
                    wrapper.or().eq(QuestionBank::getId, Long.valueOf(normalizedKeyword));
                } catch (NumberFormatException ignored) {
                    // 解析失败说明关键词不是数字，此时保留名称查询，不追加 ID 条件。
                }
            });
        }

        // status 非空时添加状态等值条件。
        query.eq(status != null, QuestionBank::getStatus, status)
                // assignedOnly 为 true 时，只允许查询分配给当前管理员的题库 ID。
                .in(assignedOnly, QuestionBank::getId, allowedBankIds)
                // 选择了分类时，只允许查询位于目标子模块范围内的题库。
                .in(categorySubModuleIds != null, QuestionBank::getSubModuleId, categorySubModuleIds);

        // SORT_ORDER_DESC 模式使用题库人工权重作为第一排序字段。
        if ("SORT_ORDER_DESC".equals(selectSortMode)) {
            // 按 question_bank.sort_order 降序排列。
            query.orderByDesc(QuestionBank::getSortOrder);
        } else {
            // 默认使用更新时间作为第一排序字段。
            query.orderByDesc(QuestionBank::getUpdatedTime);
        }
        // 把题库 ID 作为第二排序字段，第一字段相同时按较新 ID 优先。
        query.orderByDesc(QuestionBank::getId);

        // 使用前面组装的条件执行 question_bank 分页查询。
        Page<QuestionBank> page = bankMapper.selectPage(new Page<>(selectPage, defaultSize), query);
        // 从 MyBatis-Plus 分页结果中取出当前页的题库实体。
        List<QuestionBank> questionBanks = page.getRecords();
        // 把当前页题库实体逐条转换成前端列表所需的 VO。
        List<QuestionBankRowVO> questionBankRowVOS = questionBanks.stream()
                .map(assembler::toRow)
                .toList();

        // 创建项目统一使用的分页响应对象。
        PageResult<QuestionBankRowVO> result = new PageResult<>();
        // 设置转换后的当前页记录。
        result.setRecords(questionBankRowVOS);
        // 设置数据库分页查询得到的总记录数。
        result.setTotal(page.getTotal());
        // 设置 MyBatis-Plus 返回的当前页码。
        result.setPageNum(page.getCurrent());
        // 设置 MyBatis-Plus 返回的每页数量。
        result.setPageSize(page.getSize());
        // 返回普通题库分页结果。
        return result;
    }

    /** 查询未删除题库的详情。 */
    public QuestionBankDetailVO get(Long bankId) {
        // 校验当前管理员是否拥有该题库的数据访问范围。
        accessService.requireBank(bankId);
        // 使用普通 BaseMapper 查询，因此已逻辑删除的题库不会被返回。
        QuestionBank bank = bankMapper.selectById(bankId);
        // ID 不存在或题库已经删除时向前端返回“题库不存在”业务异常。
        if (bank == null) {
            // 抛出项目统一的管理端题库不存在异常。
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NOT_FOUND);
        }
        // 把题库实体及关联信息组装成详情 VO 后返回。
        return assembler.toDetail(bank);
    }

    /** 创建一个草稿题库，并保存标签、管理员数据范围和审计记录。 */
    @Transactional
    public QuestionBankDetailVO create(QuestionBankCreateDTO dto) {
        // 校验前端提交的子模块确实存在且未被逻辑删除。
        CategorySubModule subModule = subModuleMapper.selectById(dto.getSubModuleId());
        // 子模块不存在时不能创建题库。
        if (subModule == null) {
            // 抛出题库分类不合法业务异常。
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_CATEGORY_INVALID);
        }

        // 查询同一 SubModule 中是否已经存在同名且未删除的题库。
        Long sameName = bankMapper.selectCount(new LambdaQueryWrapper<QuestionBank>()
                .eq(QuestionBank::getSubModuleId, dto.getSubModuleId()) // 如果想要创建题库，是一定选择并进入到一个确定的subModule的，所以subModuleId一定是已知的
                .eq(QuestionBank::getBankName, dto.getBankName().trim())); // 使用去除首尾空白后的题库名称做精确匹配。
        if (sameName > 0) { // 已存在同名题库时拒绝创建。
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NAME_CONFLICT);
        }

        // 如果等于0，说明这个名称 在当前subModule的 未删除题库中 不存在
        // 那么可以创建新的题库
        QuestionBank bank = new QuestionBank();
        bank.setBankName(dto.getBankName().trim());
        bank.setSubModuleId(dto.getSubModuleId());// 保存题库所属的最末级分类 ID。
        bank.setCompleteCount(0);// 新题库初始完成人数为 0。
        bank.setAvgCorrectRate(java.math.BigDecimal.ZERO);// 新题库初始平均正确率为 0。
        bank.setViewCount(0);// 新题库初始浏览次数为 0。


        int sortOrder = 10;// 前端未传排序权重时使用普通题库默认值 10。
        if (dto.getSortOrder() != null) {
            sortOrder = dto.getSortOrder();
        }
        bank.setSortOrder(sortOrder);
        bank.setCreateAdminId(AdminContext.getAdminId());// 记录创建该题库的后台管理员 ID。
        bank.setStatus(QuestionBankStatus.DRAFT);// 新创建的题库默认处于草稿状态。
        bank.setVersion(0);// 初始化乐观锁版本号。

        // 把题库实体插入 question_bank 表，插入后实体会得到主键 ID。
        // 因为 tagNames 存入到 bank_tag 表中，每一个tag 都要与一个 bankId 对应，因此要先插入新的bank，生成bankId。
        bankMapper.insert(bank);

        // 默认使用空标签列表，避免 dto.tagNames 为 null 时出现空指针。
        List<String> tagNames = List.of();
        // 前端传入标签列表时才执行清洗和去重。
        if (dto.getTagNames() != null) {
            // 把标签列表中的首尾空字符串去掉 并 去重
            tagNames = dto.getTagNames().stream().map(String::strip).distinct().toList();
        }

        // 为每个清洗后的标签创建一条 bank_tag 记录。
        for (String tagName : tagNames) {
            BankTag tag = new BankTag();
            tag.setBankId(bank.getId());
            tag.setTagName(tagName);
            bankTagMapper.insert(tag);
        }

        // 受 ASSIGNED_BANKS 限制的普通管理员创建题库后，需要自动获得该题库的数据范围。
        if (AdminContext.get().getRole() != AdminRole.SUPER_ADMIN && AdminContext.get().getBankDataScope() == BankDataScope.ASSIGNED_BANKS) {
            // 创建管理员与新题库的数据范围关联。
            AdminBankScope scope = new AdminBankScope();
            // 设置获得访问权的管理员 ID。
            scope.setAdminId(AdminContext.getAdminId());
            // 设置刚创建的题库 ID。
            scope.setBankId(bank.getId());
            // 保存管理员题库范围关联。
            bankScopeMapper.insert(scope);
        }

        // 记录题库创建审计日志；before 为 null，after 为新题库实体。
        auditService.record("BANK", "CREATE", "QUESTION_BANK", bank.getId(), "创建题库", null, bank);
        // 重新读取新题库并组装完整详情后返回。
        return assembler.toDetail(bankMapper.selectById(bank.getId()));
    }

    /** 修改题库名称、分类、标签和人工排序权重。 */
    @Transactional
    public QuestionBankDetailVO update(Long bankId, QuestionBankUpdateDTO dto) {
        // 校验当前管理员是否拥有目标题库的数据访问范围。
        accessService.requireBank(bankId);
        // 查询未被逻辑删除的目标题库。
        QuestionBank bank = bankMapper.selectById(bankId);
        // 题库不存在或已删除时拒绝更新。
        if (bank == null) {
            // 返回题库不存在业务异常。
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NOT_FOUND);
        }

        // 比较客户端版本号和数据库当前版本号，避免覆盖其他管理员的并发修改。
        if (!bank.getVersion().equals(dto.getVersion())) {
            // 版本不一致时要求前端刷新数据后重试。
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }

        // 查询更新后准备归属的目标子模块。
        CategorySubModule targetSubModule = subModuleMapper.selectById(dto.getSubModuleId());
        // 目标子模块不存在时拒绝更新分类。
        if (targetSubModule == null) {
            // 返回题库分类不合法业务异常。
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_CATEGORY_INVALID);
        }

        // 查询题库修改前所属的一级分类类型。
        GroupType currentGroup = bankMapper.selectGroupType(bankId);
        // 根据目标子模块查询其所属模块。
        CategoryModule targetModule = moduleMapper.selectById(targetSubModule.getModuleId());
        // 预设目标一级分类为空，只有分类链路完整时才会得到具体值。
        CategoryGroup targetGroup = null;
        // 目标模块存在时继续向上查询目标 Group。
        if (targetModule != null) {
            // 根据模块的 groupId 查询目标 Group。
            targetGroup = groupMapper.selectById(targetModule.getGroupId());
        }
        // 模块或 Group 不存在说明目标分类链路不完整。
        if (targetGroup == null) {
            // 返回题库分类不合法业务异常。
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_CATEGORY_INVALID);
        }

        // 分别统计两种题目表中属于当前题库的题目数量，再计算总题目数。
        long questionCount = interviewQuestionMapper.selectCount(
                // 统计 interview_question_info 中 bank_id 等于当前题库的记录。
                new LambdaQueryWrapper<InterviewQuestionInfo>()
                        .eq(InterviewQuestionInfo::getBankId, bankId)
        ) + certificateQuestionMapper.selectCount(
                // 统计 certificate_question_info 中 bank_id 等于当前题库的记录。
                new LambdaQueryWrapper<CertificateQuestionInfo>()
                        .eq(CertificateQuestionInfo::getBankId, bankId)
        );
        // 已有题目时不允许跨 INTERVIEW/CERTIFICATION 一级分类移动题库。
        if (questionCount > 0 && currentGroup != targetGroup.getGroupType()) {
            // 跨一级类型会导致题库与题目实体类型不一致，因此拒绝更新。
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_CATEGORY_INVALID);
        }

        // 查询目标 SubModule 中除当前题库以外是否存在同名且未删除的题库。
        Long sameName = bankMapper.selectCount(new LambdaQueryWrapper<QuestionBank>()
                // 修改分类时按照更新后的 SubModule 判断名称是否重复。
                .eq(QuestionBank::getSubModuleId, dto.getSubModuleId())
                // 使用清理后的新名称做精确匹配。
                .eq(QuestionBank::getBankName, dto.getBankName().trim())
                // 排除当前正在修改的题库自身。
                .ne(QuestionBank::getId, bankId));
        // 其他题库已经使用相同名称时拒绝更新。
        if (sameName > 0) {
            // 返回题库名称冲突业务异常。
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NAME_CONFLICT);
        }

        // 创建修改前快照，供审计日志记录字段变化。
        QuestionBank before = new QuestionBank();
        // 把当前题库属性复制到修改前快照。
        BeanUtils.copyProperties(bank, before);
        // 更新清理后的题库名称。
        bank.setBankName(dto.getBankName().trim());
        // 更新题库所属子模块。
        bank.setSubModuleId(dto.getSubModuleId());

        // 前端未传排序权重时使用默认值 10。
        int sortOrder = 10;
        // 前端传入排序权重时覆盖默认值。
        if (dto.getSortOrder() != null) {
            // 使用经过 DTO 范围校验的排序权重。
            sortOrder = dto.getSortOrder();
        }
        // 保存最终人工排序权重。
        bank.setSortOrder(sortOrder);

        // updateById 会携带 @Version 版本条件并自动递增版本号。
        if (bankMapper.updateById(bank) == 0) {
            // 更新 0 行说明版本已变化或记录已不存在。
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }

        // 删除当前题库原有的全部标签，后面按请求内容重新建立。
        bankTagMapper.delete(new LambdaQueryWrapper<BankTag>().eq(BankTag::getBankId, bankId));
        // 默认使用空标签列表，表示请求未传标签时题库更新后没有标签。
        List<String> tags = List.of();
        // 前端传入标签时执行清洗和去重。
        if (dto.getTags() != null) {
            // 去除标签首尾空白，并按首次出现顺序去重。
            tags = dto.getTags().stream()
                    .map(String::strip)
                    .distinct()
                    .toList();
        }

        // 为更新后的每个标签重新创建 bank_tag 记录。
        for (String tagName : tags) {
            // 创建标签关联实体。
            BankTag tag = new BankTag();
            // 关联当前正在更新的题库 ID。
            tag.setBankId(bankId);
            // 保存清理后的标签名称。
            tag.setTagName(tagName);
            // 插入新的标签记录。
            bankTagMapper.insert(tag);
        }

        // 记录题库更新审计日志，包含修改前快照和修改后实体。
        auditService.record("BANK", "UPDATE", "QUESTION_BANK", bankId, dto.getReason(), before, bank);
        // 重新读取题库并组装更新后的完整详情。
        return assembler.toDetail(bankMapper.selectById(bankId));
    }

    /** 发布、下架或删除题库。 */
    @Transactional
    public ActionResultVO action(Long bankId, ResourceActionDTO dto) {
        // 校验当前管理员是否拥有目标题库的数据访问范围。
        accessService.requireBank(bankId);
        // 只查询未删除题库；题库级恢复功能已经取消。
        QuestionBank bank = bankMapper.selectById(bankId);
        // 题库不存在或已删除时拒绝执行动作。
        if (bank == null) {
            // 返回题库不存在业务异常。
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NOT_FOUND);
        }

        // 校验客户端版本，防止在旧数据上执行状态动作。
        if (!bank.getVersion().equals(dto.getVersion())) {
            // 版本不一致时要求前端刷新后重试。
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }

        // 清理动作名称首尾空白并统一转成大写，方便后面进行固定字符串匹配。
        String action = dto.getAction().trim().toUpperCase(Locale.ROOT);
        // 创建动作执行前的题库快照，供审计日志使用。
        QuestionBank before = new QuestionBank();
        // 复制动作执行前的题库属性。
        org.springframework.beans.BeanUtils.copyProperties(bank, before);

        // PUBLISH 分支负责把草稿或已下架题库发布上线。
        if ("PUBLISH".equals(action)) {
            // 发布动作要求当前管理员拥有 bank:publish 权限。
            accessService.requirePermission("bank:publish");
            // 已删除、非草稿且非下架状态的题库都不允许发布。
            if (Boolean.TRUE.equals(bank.getDeleted())
                    || (bank.getStatus() != QuestionBankStatus.DRAFT
                    && bank.getStatus() != QuestionBankStatus.OFFLINE)) {
                // 当前题库状态不支持发布时返回状态异常。
                throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_STATE_INVALID);
            }

            // 预设已发布题目数量为 0，后面根据题库一级类型选择对应题目表统计。
            long released = 0;
            // 沿题库分类链路查询其所属的 INTERVIEW 或 CERTIFICATION 类型。
            GroupType groupType = bankMapper.selectGroupType(bankId);
            // 面试题库从 interview_question_info 表统计已发布题目。
            if (groupType == GroupType.INTERVIEW) {
                // 统计当前题库中 isReleased=true 的面试题目数量。
                released = interviewQuestionMapper.selectCount(
                        new LambdaQueryWrapper<InterviewQuestionInfo>()
                                // 限定题目必须属于当前题库。
                                .eq(InterviewQuestionInfo::getBankId, bankId)
                                // 限定题目已经发布。
                                .eq(InterviewQuestionInfo::getIsReleased, true));
            }
            // 认证题库从 certificate_question_info 表统计已发布题目。
            if (groupType == GroupType.CERTIFICATION) {
                // 统计当前题库中 isReleased=true 的认证题目数量。
                released = certificateQuestionMapper.selectCount(
                        new LambdaQueryWrapper<CertificateQuestionInfo>()
                                // 限定题目必须属于当前题库。
                                .eq(CertificateQuestionInfo::getBankId, bankId)
                                // 限定题目已经发布。
                                .eq(CertificateQuestionInfo::getIsReleased, true));
            }
            // 没有任何已发布题目时不允许发布整个题库。
            if (released == 0) {
                // 返回“题库没有可发布题目”业务异常。
                throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NO_RELEASED_QUESTION);
            }

            // 把题库状态修改为已发布。
            bank.setStatus(QuestionBankStatus.PUBLISHED);
            // 题库第一次发布时记录发布时间，重新上线时保留第一次发布时间。
            if (bank.getPublishedTime() == null) {
                // 使用当前服务器时间作为首次发布时间。
                bank.setPublishedTime(LocalDateTime.now());
            }
            // 更新题库状态，并通过 @Version 防止并发覆盖。
            if (bankMapper.updateById(bank) == 0) {
                // 更新 0 行表示版本冲突或记录状态已经变化。
                throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
            }

        // OFFLINE 分支负责把已发布题库下架。
        } else if ("OFFLINE".equals(action)) {
            // 下架动作同样要求 bank:publish 权限。
            accessService.requirePermission("bank:publish");
            // 只有未删除且当前为 PUBLISHED 的题库才能下架。
            if (Boolean.TRUE.equals(bank.getDeleted()) || bank.getStatus() != QuestionBankStatus.PUBLISHED) {
                // 当前状态不允许下架时返回状态异常。
                throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_STATE_INVALID);
            }
            // 把题库状态修改为已下架。
            bank.setStatus(QuestionBankStatus.OFFLINE);
            // 更新题库状态，并通过 @Version 防止并发覆盖。
            if (bankMapper.updateById(bank) == 0) {
                // 更新 0 行表示版本冲突或记录已变化。
                throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
            }

        // DELETE 分支负责逻辑删除草稿或已下架题库。
        } else if ("DELETE".equals(action)) {
            // 删除动作要求当前管理员拥有 bank:delete 权限。
            accessService.requirePermission("bank:delete");
            // 已删除题库不能重复删除，已发布题库必须先下架再删除。
            if (Boolean.TRUE.equals(bank.getDeleted()) || bank.getStatus() == QuestionBankStatus.PUBLISHED) {
                // 当前状态不允许删除时返回状态异常。
                throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_STATE_INVALID);
            }
            // 使用自定义 SQL 按版本号执行逻辑删除，并保存删除原因。
            if (bankMapper.logicalDelete(bankId, dto.getReason(), dto.getVersion()) == 0) {
                // 删除 0 行表示版本冲突或记录状态已经变化。
                throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
            }

        // 不支持的动作名称属于请求参数错误。
        } else {
            // 返回统一的参数错误业务异常。
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        // 动作完成后重新读取包含逻辑删除状态的最新题库记录。
        QuestionBank updated = bankMapper.selectIncludingDeleted(bankId);
        // 记录状态动作审计日志，保存动作前后快照和操作原因。
        auditService.record("BANK", action, "QUESTION_BANK", bankId, dto.getReason(), before, updated);

        // 创建返回给前端的状态动作结果对象。
        ActionResultVO result = new ActionResultVO();
        // 返回本次操作的题库 ID。
        result.setTargetId(bankId);
        // 返回规范化后的动作名称。
        result.setAction(action);
        // 返回动作完成后的题库状态。
        result.setStatus(updated.getStatus().name());
        // 返回动作完成后的最新版本号，供前端下一次操作使用。
        result.setVersion(updated.getVersion());
        // 返回动作完成后的最后更新时间。
        result.setUpdatedTime(updated.getUpdatedTime());
        // 返回完整的动作执行结果。
        return result;
    }
}
