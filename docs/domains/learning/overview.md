# 学习与做题

## 1. 功能目标

学习域负责用户从进入题库到完成、复习的完整过程，包括面试练习、认证练习、认证考试、答题记录和 AI 辅助。

## 2. 主流程

```text
选择题库 → 进入练习或考试 → 保存/提交答案 → 查看评价或解析
         → 完成题库 → 复习答题记录 → 管理个人学习资产
```

## 3. 子模块

- [做题模式](assessment-modes.md)：面试练习、认证练习、认证考试。
- [学习资产](learning-assets.md)：错题、收藏题目、笔记、学习日历和统计。
- AI 辅助：面试答案评价、参考答案和基于题目上下文的后续追问。
- 复习：按题库回顾历史答案、解析和完成情况。

## 4. 边界

- 分类、题库、题目和发布状态属于[题库内容](../question-content/overview.md)。
- 收藏题目属于学习资产；收藏 Hit 属于[社交互动](../social/interactions.md)。
- 会员可能控制题库访问权限，但价格、订单和权益状态属于[会员与交易](../membership/overview.md)。

## 5. 接口与实现

- 练习与复习：`/api/app/bank/questions/**`
- 认证考试：`/api/app/bank/certificate/exams/**`
- 前端：`features/question`、`features/question-bank`
- 后端：`QuestionInfoController`、`CertificateExamController`
