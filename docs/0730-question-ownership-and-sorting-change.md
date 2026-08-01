# 0730 题库、题目归属与排序改造说明

## 1. 改造结果

本次改造把题目模型从“一题多库关系表”调整为“一题只属于一个题库”：

```text
question_bank
    ├── interview_question_info.bank_id
    └── certificate_question_info.bank_id
```

- 删除 `question_bank_question` 表及其前后端 Mapper、Service、VO 和测试。
- 原 `question_info` 表改名为 `interview_question_info`。
- 两张题目表直接保存 `bank_id` 和 `sort_order`。
- 同一内容需要出现在 A、B 两个题库时，分别创建两条题目记录，使用不同的题目 ID。
- `question_bank.sort_order` 表示 App 端的人工曝光权重，默认 10，数值越大越优先。
- 题目表的 `sort_order` 表示题库内人工顺序，默认 10，数值越小越靠前。
- `sortMode` 是接口请求参数，不是数据库字段。

## 2. 数据库修改

修改脚本：

- `sql/question_ownership_single_bank_migration.sql`

脚本执行的操作：

1. 检查历史数据中是否存在同一道题关联多个题库。
2. 将 `question_info` 改名为 `interview_question_info`。
3. 将 `question_bank.priority` 改名为 `question_bank.sort_order`，并设置为
   `INT NOT NULL DEFAULT 10`。
4. 给 `interview_question_info` 和 `certificate_question_info` 增加 `bank_id`。
5. 将两张题目表的 `sort_order` 设置为 `INT NOT NULL DEFAULT 10`。
6. 从旧关系表回填每道题的 `bank_id` 和 `sort_order`。
7. 校验是否存在没有题库归属的题目。
8. 给两张题目表的 `bank_id` 增加非空约束、外键和列表查询索引。
9. 删除 `question_bank_question`。

新增的核心索引：

```sql
(bank_id, is_deleted, updated_time, id)
(bank_id, is_deleted, sort_order, id)
```

没有给 `(bank_id, sort_order)` 添加唯一索引。原因是默认值可以重复，真正拖拽保存时才会统一重写为
`10、20、30...`。

## 3. 实体模型修改

| 修改位置 | 修改内容 | 现在的用途 |
| --- | --- | --- |
| `InterviewQuestionInfo` | 表名改为 `interview_question_info`；增加 `bankId`；保留 `sortOrder` | 直接保存面试题的所属题库和题库内顺序 |
| `CertificateQuestionInfo` | 增加 `bankId`；保留 `sortOrder` | 直接保存认证题的所属题库和题库内顺序 |
| `QuestionBank` | `priority` 改为 `sortOrder` | App 端人工曝光权重 |
| `QuestionBankQuestion` | 删除 | 不再维护题库与题目的多对多关系 |
| `ItemType` | 补回 `MODULE(1, "module")` | 修复原有模块图片查询引用枚举但枚举值缺失的构建问题 |

## 4. 管理端后端修改

### 4.1 题库列表

修改位置：

- `AdminQuestionBankController.list`
- `AdminQuestionBankService.list`

接口删除：

```text
sortBy
sortDirection
```

接口新增：

```text
sortMode
```

允许的题库排序模式：

| `sortMode` | LambdaQueryWrapper 排序 |
| --- | --- |
| `UPDATED_TIME_DESC` | `question_bank.updated_time DESC, question_bank.id DESC` |
| `SORT_ORDER_DESC` | `question_bank.sort_order DESC, question_bank.id DESC` |

未传 `sortMode` 时默认使用 `UPDATED_TIME_DESC`。非法值返回 `PARAM_ERROR`，不静默降级。

题库列表还增加了 `moduleId` 和 `subModuleId` 筛选。服务直接根据分类树计算可用的
`sub_module_id`，校验题库类型、模块和子模块属于同一条分类路径后，再通过
`LambdaQueryWrapper.in` 查询题库。

### 4.2 题库创建和修改

修改位置：

- `QuestionBankCreateDTO`
- `QuestionBankUpdateDTO`
- `QuestionBankRowVO`
- `AdminQuestionBankService.create`
- `AdminQuestionBankService.update`
- `QuestionBankAssembler`

请求和响应字段从 `priority` 改为 `sortOrder`。创建或修改时没有传值则写入 10。

题库切换分类类型和发布题库时，不再查询关系表：

- 面试题库直接统计 `interview_question_info.bank_id`。
- 认证题库直接统计 `certificate_question_info.bank_id`。

### 4.3 题目列表

修改位置：

- `AdminQuestionController.list`
- `AdminQuestionService.list`
- `QuestionRowVO`
- `QuestionDetailVO`
- `QuestionAssembler`

允许的题目排序模式：

| `sortMode` | LambdaQueryWrapper 排序 |
| --- | --- |
| `UPDATED_TIME_DESC` | 题目表 `updated_time DESC, id DESC` |
| `MANUAL_ORDER_ASC` | 题目表 `sort_order ASC, id ASC` |

查询条件直接使用题目表的 `bank_id`，不再先查询 `question_bank_question`。

响应字段从 `bankSortOrder` 改为 `sortOrder`，并删除：

```text
referencedBankCount
visibleReferencedBanks
hasHiddenReferences
```

### 4.4 题目创建、修改和删除

修改位置：

- `AdminQuestionService.create`
- `AdminQuestionService.update`
- `AdminQuestionService.action`
- `InterviewQuestionMapper`
- `CertificateQuestionMapper`

使用的方法：

- `LambdaQueryWrapper.eq(bankId).eq(questionId)`：同时校验题目 ID 和所属题库，防止跨题库操作。
- `selectMaxSortOrder(bankId)`：读取当前题库未删除题目的最大顺序。
- `logicalDelete(bankId, questionId, version)`：校验题库归属和乐观锁版本后逻辑删除。
- `@Transactional`：题目写入、图片绑定、状态变化和审计保持在同一事务边界内。

顺序规则：

- 创建题目：`MAX(sort_order) + 10`。
- 删除题目：不立即整理其他题目的顺序。
- 删除记录仅作为历史数据保留，管理端不提供回收站和恢复功能。

### 4.5 拖拽调整题目顺序

修改位置：

- `AdminQuestionService.updateOrder`
- `InterviewQuestionMapper.updateSortOrder`
- `CertificateQuestionMapper.updateSortOrder`

保存过程：

1. 前端读取当前题库的全部未删除题目。
2. 前端提交拖拽后的全部题目 ID。
3. 后端检查 ID 是否缺失、重复或属于其他题库。
4. 后端通过 `QuestionBankMapper.bumpVersion` 校验并递增题库版本，阻止并发覆盖。
5. 后端按数组顺序把题目 `sort_order` 重写为 `10、20、30...`。
6. 任意一条更新失败时，由 `@Transactional` 回滚全部更新。

这意味着每次成功保存后，当前有效题目的顺序值都会不同；但数据库不依赖唯一约束保证这一点。

## 5. App 后端修改

### 5.1 题库列表

修改位置：

- `QuestionBankServiceImpl`

App 的 Hot 排序：

```text
question_bank.sort_order DESC
question_bank.hot_score DESC
question_bank.id DESC
```

App 的 Latest 排序：

```text
question_bank.sort_order DESC
question_bank.created_time DESC
question_bank.id DESC
```

通常所有题库的 `sort_order` 都是 10，因此仍由 Hot 或 Latest 决定顺序。只有确实需要强曝光的题库才设置更高权重。

### 5.2 题目读取

修改位置：

- `QuestionInfoServiceImpl`
- `AiPromptBuilder`
- `CertificateExamServiceImpl`

改造后的查询方式：

- 练习、复习、清除记录、收藏、笔记、提交答案和 AI 提示词都直接校验
  `题目表.bank_id = bankId`。
- 面试题和认证题的普通展示使用
  `sort_order ASC, id ASC`，避免未指定 `ORDER BY` 导致返回顺序不稳定。
- 认证考试创建 Session 时仍会随机题序，不受后台人工顺序限制。

已删除：

- App 端 `QuestionBankQuestionMapper`
- `QuestionBankOrderService`
- `QuestionBankOrderServiceTest`

## 6. 管理端前端修改

### 6.1 题库页面

修改位置：

- `frontend/web-admin/src/views/banks/BankListView.vue`
- `frontend/web-admin/src/api/admin.ts`
- `frontend/web-admin/src/types/admin.ts`

新增可选项：

- 题库类型。
- 模块。
- 子模块。
- 按更新时间降序。
- 按题库权重降序。

创建题库和编辑题库都使用 `sortOrder`，默认显示 10。

### 6.2 题目页面

修改位置：

- `frontend/web-admin/src/views/banks/BankWorkspaceView.vue`
- `frontend/web-admin/src/views/questions/QuestionFormView.vue`

新增可选项：

- 按更新时间降序。
- 按手动顺序。

“调整顺序”继续调用：

```http
PUT /api/admin/question-banks/{bankId}/questions/order
```

打开调整弹窗时固定用 `MANUAL_ORDER_ASC` 加载全部题目，保存时提交完整 ID 数组。

页面已删除共享题目的引用数量、隐藏引用提示和共享题删除限制。

## 7. 可读性处理

- 核心业务流程直接写在对应公开 Service 方法中，没有新增多层私有辅助方法进行跳转。
- 面试题和认证题分支在同一个业务方法内完整展开，便于逐行阅读两种数据模型的差异。
- 变更处使用“变更：原来……；现在……”形式的注释说明旧行为和新行为。
- 查询优先使用类型安全的 `LambdaQueryWrapper`；必须读取逻辑删除记录、执行带版本条件更新或批量重排时，才使用显式 Mapper SQL。
- 排序都增加 `id` 作为最终稳定排序字段，避免相同时间或相同默认顺序时列表随机变化。

## 8. 数据库部署顺序

本次迁移包含表改名和删除关系表，不能在旧版 App/Admin 仍运行时执行。

建议按以下顺序部署：

1. 备份生产数据库。
2. 确认新版本后端和前端构建产物已经准备完成。
3. 进入维护窗口并停止旧版 `web-app`、`web-admin`。
4. 确认该环境尚未应用的历史迁移已经按版本顺序执行完成；不要重复执行已应用的脚本。然后执行：

   ```bash
   mysql -u DB_USER -p DB_NAME < sql/question_ownership_single_bank_migration.sql
   ```

5. 第一条“多题库引用检查”必须返回空结果。
6. 两条“题目没有 bank_id”检查必须都返回 0，才能继续收紧约束和删除旧关系表。
7. 部署并启动本次配套的新版本后端。
8. 部署本次配套的管理端和 App 前端。
9. 分别验证题库列表、题目列表、创建、删除、拖拽排序和 App 刷题。

如果任一迁移前检查不满足，不要执行后续 DDL；先恢复或处理异常数据。

## 9. 验证结果

已执行：

```text
mvn -f backend/pom.xml test
pnpm typecheck
pnpm --filter homework-web-admin test
pnpm build:frontend
git diff --check
```

结果：

- 后端共 48 个测试通过：`common 2 + web-admin 7 + web-app 39`。
- 管理端前端测试：1 个通过。
- `web-admin` 和 `web-app` 类型检查通过。
- `web-admin` 和 `web-app` 生产构建通过。
- `git diff --check` 通过。

`pnpm test:frontend` 中，`web-app` 现有 6 个测试因 Vitest 未注册 `jest-dom` 匹配器失败，
错误均为 `Invalid Chai property: toBeInTheDocument/toHaveClass/toHaveAttribute`。
该问题与本次题库/题目改造无关，本次没有扩大范围修改测试基础设施。
