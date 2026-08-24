# 题库内容

## 1. 功能目标

题库内容域描述 HomeWork 中可学习的内容及其组织方式。它负责“有什么题”，不负责用户作答、考试和复习过程。

## 2. 内容层级

```text
分类组 → 模块 → 子模块 → 题库 → 题目 → 选项/图片
```

- 分类组通过 `GroupType` 区分面试题库和认证题库。
- 题库归属于明确的分类层级。
- 题目只归属于一个题库，并在题库内显式排序。
- 面试题以简答为主；认证题支持单选、多选等客观题型。

## 3. 生命周期

题库和题目具有草稿、发布、下架或删除等状态。管理端负责状态动作，用户端只读取当前用户有权访问的已发布内容。

详细发布约束参见[内容发布](publishing.md)，用户答题过程参见[学习与做题](../learning/overview.md)。

## 4. 接口与实现

- 用户端题库目录：`/api/app/question-banks/**`
- 用户端题目读取：`/api/app/bank/questions/**`
- 管理端题库：`/api/admin/question-banks/**`
- 管理端题目：`/api/admin/question-banks/{bankId}/questions/**`
- 数据实体：`QuestionBank`、`InterviewQuestionInfo`、`CertificateQuestionInfo`、`QuestionOption`
