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
import com.homework.model.enums.AdminSortMode;
import com.homework.model.enums.BankDataScope;
import com.homework.model.enums.GroupType;
import com.homework.model.enums.QuestionBankStatus;
import com.homework.model.enums.QuestionInfoStatus;
import com.homework.web.admin.auth.AdminAccessService;
import com.homework.web.admin.context.AdminContext;
import com.homework.web.admin.dto.QuestionBankActionDTO;
import com.homework.web.admin.dto.QuestionBankCreateDTO;
import com.homework.web.admin.dto.QuestionBankUpdateDTO;
import com.homework.model.enums.QuestionBankAction;
import com.homework.web.admin.mapper.AdminBankScopeMapper;
import com.homework.web.admin.mapper.BankTagMapper;
import com.homework.web.admin.mapper.CategoryGroupMapper;
import com.homework.web.admin.mapper.CategoryModuleMapper;
import com.homework.web.admin.mapper.CategorySubModuleMapper;
import com.homework.web.admin.mapper.CertificateQuestionMapper;
import com.homework.web.admin.mapper.InterviewQuestionMapper;
import com.homework.web.admin.mapper.QuestionBankMapper;
import com.homework.web.admin.vo.ActionResultVO;
import com.homework.web.admin.vo.QuestionBankRowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
            AdminSortMode sortMode
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

        // 前端未传排序模式时，默认按更新时间降序。
        AdminSortMode selectedSortMode = sortMode == null ? AdminSortMode.UPDATED_TIME_DESC : sortMode;
        // 题库列表不支持题目专用的手动排序模式。
        if (selectedSortMode != AdminSortMode.UPDATED_TIME_DESC
                && selectedSortMode != AdminSortMode.SORT_ORDER_DESC) {
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


        // 查看管理员是否有查询题库的权限
        // 非超级管理员且数据范围为 ASSIGNED_BANKS 时，只能查看明确分配给自己的题库。
        boolean assignedOnly = AdminContext.get().getRole() != AdminRole.SUPER_ADMIN && AdminContext.get().getBankDataScope() == BankDataScope.ASSIGNED_BANKS;

        // 管理员只能看已分配题库，但 allowedBankIds 为空，说明目前没有给他分配任何题库
        // 此时要直接返回空分页，因为 allowedBankIds = [] → 无法安全生成 IN 条件 → 提前返回空分页
        if (assignedOnly && allowedBankIds.isEmpty()) {
            PageResult<QuestionBankRowVO> empty = new PageResult<>();
            empty.setRecords(List.of());
            empty.setTotal(0);
            empty.setPageNum(selectPage);
            empty.setPageSize(defaultSize);
            return empty;
        }

        //接下来，汇总查询条件，准备开始查询
        LambdaQueryWrapper<QuestionBank> query = new LambdaQueryWrapper<>();

        //查 是否传入了 关键词
        if (keyword != null && !keyword.isBlank()) {
            String normalizedKeyword = keyword.trim();
            query.like(QuestionBank::getBankName, normalizedKeyword);
        }

        //查 是否传了 题库发布状态、管理员自己的权限题库、是否传了题库类型/模块/子模块
        query.eq(status != null, QuestionBank::getStatus, status)
                // assignedOnly 为 true 时，只允许查询分配给当前管理员的题库 ID。
                .in(assignedOnly, QuestionBank::getId, allowedBankIds)
                // 查 是否传了 题库类型/模块/子模块 中的任意一个或多个（最终都转换成了 categorySubModuleIds，以方便查找 bankId）
                .in(categorySubModuleIds != null, QuestionBank::getSubModuleId, categorySubModuleIds);

        //查 是否传了 题库排序方式
        if (selectedSortMode == AdminSortMode.SORT_ORDER_DESC) {
            query.orderByDesc(QuestionBank::getSortOrder);
        } else {
            query.orderByDesc(QuestionBank::getUpdatedTime);
        }
        // 把题库 ID 作为第二排序字段，第一字段相同时按较新 ID 优先。
        query.orderByDesc(QuestionBank::getId);

        // 使用前面组装的条件执行 question_bank 分页查询。
        Page<QuestionBank> page = bankMapper.selectPage(new Page<>(selectPage, defaultSize), query);
        // 从 MyBatis-Plus 分页结果中取出当前页的题库实体。
        List<QuestionBank> questionBanks = page.getRecords();

        // 弊端：每个题库分页查询2次，关联查询6次，有性能损耗（先用着）
        List<QuestionBankRowVO> questionBankRowVOS = questionBanks.stream()
                .map(assembler::toRow)
                .toList();

        PageResult<QuestionBankRowVO> result = new PageResult<>();
        result.setRecords(questionBankRowVOS);
        result.setTotal(page.getTotal());
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        return result;
    }

    // 点击进入某个具体的题库；
    public QuestionBankRowVO get(Long bankId) {
        // 校验当前管理员是否拥有该题库的数据访问范围。
        accessService.requireBank(bankId);

        QuestionBank bank = bankMapper.selectById(bankId);
        if (bank == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NOT_FOUND);
        }
        // 把题库实体及关联信息组装成题库 VO 后返回。
        return assembler.toRow(bank);
    }

    // 创建题库的构成要件：1.subModuleId;2. bankName;3. tagNames;4. sortOrder;
    @Transactional
    public QuestionBankRowVO create(QuestionBankCreateDTO dto) {

        //首先，创建题库时，一定会要求选一个 subModuleId（不然不可能知道题库建在哪个分类下）
        CategorySubModule subModule = subModuleMapper.selectById(dto.getSubModuleId());
        if (subModule == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_CATEGORY_INVALID);
        }

        // 查询同一个 SubModule 中是否已经存在同名且未删除的题库。
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
        // 到这，一个新的题库就创建完毕了

        // 接下来，是更新题库权限给对应的管理员
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
        // 重新读取新题库并组装题库 VO 后返回。
        return assembler.toRow(bank);
    }

    /** 修改题库名称、分类、标签和人工排序权重。 */
    @Transactional
    public QuestionBankRowVO update(Long bankId, QuestionBankUpdateDTO dto) {
        // 校验当前管理员是否拥有目标题库的数据访问范围。
        accessService.requireBank(bankId);
        QuestionBank bank = bankMapper.selectById(bankId);
        if (bank == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NOT_FOUND);
        }

        // 比较客户端版本号和数据库当前版本号，避免覆盖其他管理员的并发修改。
        if (!bank.getVersion().equals(dto.getVersion())) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }

        //查看当前 bank 所属的 groupType
        GroupType currentGroupType = bankMapper.selectGroupType(bankId);
        //根据 前端传入的，拟要修改的 subModuleId 查看所属的 groupType
        GroupType targetGroupType = bankMapper.selectGroupTypeBySubModuleId(dto.getSubModuleId());
        if (currentGroupType == null || targetGroupType == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_CATEGORY_INVALID);
        }

        if(targetGroupType != currentGroupType) {
            if(currentGroupType == GroupType.INTERVIEW) {
                LambdaQueryWrapper<InterviewQuestionInfo> interviewQuery = new LambdaQueryWrapper<>();
                interviewQuery.eq(InterviewQuestionInfo::getBankId,bankId);
                Long count = interviewQuestionMapper.selectCount(interviewQuery);
                if(count > 0){
                    throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_CATEGORY_INVALID);
                }
            }else if(currentGroupType == GroupType.CERTIFICATION) {
                LambdaQueryWrapper<CertificateQuestionInfo> certificateQuery = new LambdaQueryWrapper<>();
                certificateQuery.eq(CertificateQuestionInfo::getBankId,bankId);
                Long count = certificateQuestionMapper.selectCount(certificateQuery);
                if(count > 0){
                    throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_CATEGORY_INVALID);
                }
            }
        }

        // 查询目标 SubModule 中除当前题库以外是否存在同名且未删除的题库。
        Long sameName = bankMapper.selectCount(new LambdaQueryWrapper<QuestionBank>()
                .eq(QuestionBank::getSubModuleId, dto.getSubModuleId())
                .eq(QuestionBank::getBankName, dto.getBankName().trim()));
        if (sameName > 0) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NAME_CONFLICT);
        }

        // 创建修改前快照，供审计日志记录字段变化。
        QuestionBank before = new QuestionBank();
        // 把当前的 bank 信息复值给 before
        BeanUtils.copyProperties(bank, before);

        // 开始更新 bank
        bank.setBankName(dto.getBankName().trim());
        bank.setSubModuleId(dto.getSubModuleId());

        int sortOrder = 10;
        if (dto.getSortOrder() != null) {
            sortOrder = dto.getSortOrder();
        }
        bank.setSortOrder(sortOrder);

        // updateById 会携带 @Version 版本条件并自动递增版本号。
        if (bankMapper.updateById(bank) == 0) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }

        // 删除当前题库原有的全部标签，后面按请求内容重新建立。
        bankTagMapper.delete(new LambdaQueryWrapper<BankTag>().eq(BankTag::getBankId, bankId));
        // 默认使用空标签列表，表示请求未传标签时题库更新后没有标签。
        List<String> tags = List.of();
        // 前端传入标签时执行清洗和去重。
        if (dto.getTags() != null) {
            tags = dto.getTags().stream().map(String::strip).distinct().toList();
        }

        for (String tagName : tags) {
            BankTag tag = new BankTag();
            tag.setBankId(bankId);
            tag.setTagName(tagName);
            bankTagMapper.insert(tag);
        }

        // 记录题库更新审计日志，包含修改前快照和修改后实体。
        auditService.record("BANK", "UPDATE", "QUESTION_BANK", bankId, dto.getReason(), before, bank);
        // 重新读取题库并组装更新后的题库 VO。
        return assembler.toRow(bankMapper.selectById(bankId));
    }

    /** 发布、下架或删除题库。 */
    @Transactional
    public ActionResultVO action(Long bankId, QuestionBankActionDTO dto) {
        // 校验当前管理员是否拥有目标题库的数据访问范围。
        accessService.requireBank(bankId);
        QuestionBank bank = bankMapper.selectById(bankId); //去掉了 is_deleted = 1

        if (bank == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NOT_FOUND);
        }
        // 如果前端传回的 乐观锁版本号 与 数据库中记录的 不一致，则无法更新
        //失败后可以轻松重试（例如修改个人资料、编辑文章）→ 乐观锁。
        //失败后会产生复杂的业务后果或外部副作用（例如资金、库存、支付、审批等），或者希望同一时刻只有一个事务处理关键资源 → 悲观锁。
        if (!bank.getVersion().equals(dto.getVersion())) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
        }

        // DTO 已把固定请求值转换成题库动作枚举，无需再清理或解析字符串。
        QuestionBankAction action = dto.getAction();
        // 创建动作执行前的题库快照，供审计日志使用。
        QuestionBank before = new QuestionBank();
        // 复制动作执行前的题库属性。
        BeanUtils.copyProperties(bank, before);

        // PUBLISH 分支负责把草稿或已下架题库发布上线。
        if (action == QuestionBankAction.PUBLISH) {
            // 发布动作要求当前管理员拥有 bank:publish 权限。
            accessService.requirePermission("bank:publish");
            // 已删除、不是（草稿 + 已下架）状态的题库都不允许发布。
            if (Boolean.TRUE.equals(bank.getDeleted()) || (bank.getStatus() != QuestionBankStatus.DRAFT && bank.getStatus() != QuestionBankStatus.OFFLINE)) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_STATE_INVALID);
            }

            // 预设已发布题目数量为 0，后面根据题库一级类型选择对应题目表统计。
            long publishedQuestionCount = 0;
            // 沿题库分类链路查询其所属的 INTERVIEW 或 CERTIFICATION 类型。
            GroupType groupType = bankMapper.selectGroupType(bankId);
            if (groupType == GroupType.INTERVIEW) {
                publishedQuestionCount = interviewQuestionMapper.selectCount(
                        new LambdaQueryWrapper<InterviewQuestionInfo>()
                                .eq(InterviewQuestionInfo::getBankId, bankId)
                                .eq(InterviewQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED));
            }
            if (groupType == GroupType.CERTIFICATION) {
                publishedQuestionCount = certificateQuestionMapper.selectCount(
                        new LambdaQueryWrapper<CertificateQuestionInfo>()
                                .eq(CertificateQuestionInfo::getBankId, bankId)
                                .eq(CertificateQuestionInfo::getStatus, QuestionInfoStatus.PUBLISHED));
            }
            // 没有任何已发布题目时不允许发布整个题库。
            if (publishedQuestionCount == 0) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NO_RELEASED_QUESTION);
            }

            // 把题库状态修改为已发布。
            bank.setStatus(QuestionBankStatus.PUBLISHED);

            //这里应对的是 真正的乐观锁冲突（并发）
            //管理员A和B几乎同时对同一题库执行 发布 操作，B先完成，那么A的更新是失败的，因为查不到 version = 3的那一条（已经变成4了）
            //那么返回值就是0，因此这里就要设计为抛出异常，以提醒管理员A刷新页面
            if (bankMapper.updateById(bank) == 0) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
            }

        // OFFLINE 分支负责把已发布题库下架。
        } else if (action == QuestionBankAction.OFFLINE) {

            accessService.requirePermission("bank:publish");
            // 已下架、不是 已发布 状态 都不能点击 下架功能
            if (Boolean.TRUE.equals(bank.getDeleted()) || bank.getStatus() != QuestionBankStatus.PUBLISHED) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_STATE_INVALID);
            }

            bank.setStatus(QuestionBankStatus.OFFLINE);
            // 依旧是乐观锁防并发
            if (bankMapper.updateById(bank) == 0) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
            }

        // DELETE 分支负责逻辑删除草稿或已下架题库。
        } else if (action == QuestionBankAction.DELETE) {
            // 删除动作要求当前管理员拥有 bank:delete 权限。
            accessService.requirePermission("bank:delete");
            // 已删除题库不能重复删除，已发布题库必须先下架再删除。
            if (Boolean.TRUE.equals(bank.getDeleted()) || bank.getStatus() == QuestionBankStatus.PUBLISHED) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_STATE_INVALID);
            }

            //这里一定要自定义一个删除方法，因为 乐观锁版本的自增，仅限update方法，delete不能实现
            //而且自定义delete还可以写入 reason和version 字段
            if (bankMapper.logicalDelete(
                    bankId,
                    QuestionBankStatus.DELETED,
                    dto.getReason(),
                    dto.getVersion()
            ) == 0) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_RESOURCE_VERSION_CONFLICT);
            }

        } else {
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        // 动作完成后重新读取包含逻辑删除状态的最新题库记录。
        QuestionBank after = bankMapper.selectIncludingDeleted(bankId);
        // 记录状态动作审计日志，保存动作前后快照和操作原因。
        auditService.record("BANK", action.name(), "QUESTION_BANK", bankId, dto.getReason(), before, after);

        // 创建返回给前端的状态动作结果对象。
        ActionResultVO result = new ActionResultVO();
        // 返回本次操作的题库 ID。
        result.setTargetId(bankId);
        // 返回动作枚举的固定数字 value。
        result.setAction(action.getValue());
        // 返回动作完成后的题库状态。
        result.setStatus(after.getStatus().getValue());
        // 返回动作完成后的最新版本号，供前端下一次操作使用。
        result.setVersion(after.getVersion());
        // 返回动作完成后的最后更新时间。
        result.setUpdatedTime(after.getUpdatedTime());
        // 返回完整的动作执行结果。
        return result;
    }
}
