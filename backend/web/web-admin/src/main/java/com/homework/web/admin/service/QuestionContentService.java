package com.homework.web.admin.service;

import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.enums.GroupType;
import com.homework.model.enums.QuestionInfoQuestionType;
import com.homework.web.admin.dto.QuestionOptionDTO;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 统一校验并转换手工创建、编辑和 Excel 导入的题目内容。
 *
 * <p>面试题库只允许简答题，简答题不保存选项和正确答案；
 * 证书题库只允许单选题和多选题，需要校验选项与正确答案。</p>
 */
@Service
public class QuestionContentService {

    /**
     * 校验题库类型、题型、选项和正确答案是否匹配。
     *
     * <p>调用方应先确认 {@code groupType} 已根据有效题库查出，不为 {@code null}。</p>
     *
     * @param groupType      题库所属分组类型（面试或证书）
     * @param questionType   题型
     * @param options        选项；简答题可传 {@code null} 或空列表
     * @param correctAnswerKeys 正确选项的字母键；简答题可传 {@code null} 或空列表
     * @throws HomeworkException 题型与题库不匹配，或选项、正确答案不合法
     */
    public void validateQuestionCreation( //没有检查analysis, 是因为这个字段不影响题目创建，可有可无，空字符串也没事
            GroupType groupType,
            QuestionInfoQuestionType questionType,
            List<QuestionOptionDTO> options,
            List<String> correctAnswerKeys
    ) {
        if (questionType == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_TYPE_INVALID);
        }

        // 面试题库只允许简答题；证书题库只允许有标准答案的选择题。
        if (groupType == GroupType.INTERVIEW && questionType != QuestionInfoQuestionType.ESSAY) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_TYPE_INVALID);
        }
        if (groupType == GroupType.CERTIFICATION && questionType == QuestionInfoQuestionType.ESSAY) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_TYPE_INVALID);
        }

        // 简答题没有选项和标准答案，null 与空列表都表示“未提供”。
        if (questionType == QuestionInfoQuestionType.ESSAY) {
            if (options != null && !options.isEmpty() || correctAnswerKeys != null && !correctAnswerKeys.isEmpty()) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_OPTION_INVALID);
            }
            return;
        }

        // 选择题必须提供 2～6 个实际选项，并且至少指定一个正确答案。
        if (options == null || options.size() < 2 || options.size() > 6
                || correctAnswerKeys == null || correctAnswerKeys.isEmpty()) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_OPTION_INVALID);
        }

        // 选项键必须按列表顺序连续为 A、B、C…，且选项内容不得为空或重复。
        Set<String> optionContents = new HashSet<>();
        Set<String> validOptionKeys = new HashSet<>();

        for (int index = 0; index < options.size(); index++) {
            QuestionOptionDTO option = options.get(index);

            if (option == null || option.getKey() == null) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_OPTION_INVALID);
            }

            // 如何在程序中检查选项是否是按照A-F顺序排列的？
            // 只能是设定一个规则，从大写Unicode字符A开始，每次遍历+1，然后与参数中的key比对
            String expectedKey = String.valueOf((char) ('A' + index)); // 'A' + 0/1/2/3/4/5 以此类推就是 A/B/C/D/E
            String actualKey = option.getKey().trim().toUpperCase(Locale.ROOT);

            // 选项必须按照 A、B、C……排列
            if (!expectedKey.equals(actualKey)) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_OPTION_INVALID);
            }

            // 输入的选项内容不能为空
            if (option.getContent() == null || option.getContent().isBlank()) {
                throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_OPTION_INVALID);
            }

            // 选项内容不能重复
            String content = option.getContent().trim();
            if (optionContents.add(content) == false) { // 利用Hashset去重，添加失败就报错
                throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_OPTION_INVALID);
            }

            // 记录已通过校验的选项键，供后面校验正确答案的引用。
            validOptionKeys.add(expectedKey);
        }

        // 正确答案列表中如果有任何一个正确选项是 null或空，报错
        if (correctAnswerKeys.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_OPTION_INVALID);
        }

        // 到这一步，说明答案里的选项全部都是非空非null的
        // 然后trim+统一大写，然后检查 重复、非选项里的字母、单选但正确选项大于1、多选但正确选项小于2 的情况，报错
        List<String> normalizedCorrectAnswerKeys = correctAnswerKeys.stream().map(value -> value.trim().toUpperCase(Locale.ROOT)).toList();
        if (new HashSet<>(normalizedCorrectAnswerKeys).size() != normalizedCorrectAnswerKeys.size()
                || !validOptionKeys.containsAll(normalizedCorrectAnswerKeys)
                || questionType == QuestionInfoQuestionType.SINGLE_CHOICE && normalizedCorrectAnswerKeys.size() != 1
                || questionType == QuestionInfoQuestionType.MULTIPLE && normalizedCorrectAnswerKeys.size() < 2) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_QUESTION_OPTION_INVALID);
        }
    }

    /**
     * 将选项 DTO 转成按 A、B、C… 顺序存储的选项文本列表。
     *
     * <p>仅在选择题通过 {@link #validateQuestionCreation(GroupType, QuestionInfoQuestionType, List, List)}
     * 校验后调用。</p>
     */
    public List<String> toOptionContents(List<QuestionOptionDTO> options) {
        return options.stream().map(option -> option.getContent().trim()).toList();
        //核心宗旨是：存入到题目表中的是选项内容，不需要选项的key，因为可以利用List集合的特点，根据index，反序列化称为A/B/C/D 返回前端
    }

    /**
     * 将正确答案键（例如 A、C）转成对应的选项文本。
     *
     * <p>数据库保存的是正确选项内容，而不是选项字母。
     * 仅在选择题通过内容校验后调用。</p>
     *
     * @param options           按 A、B、C… 排列的选项
     * @param correctAnswerKeys 正确选项的字母键
     * @return 正确选项的文本列表
     */
    public List<String> toCorrectAnswerContents(
            List<QuestionOptionDTO> options,
            List<String> correctAnswerKeys
    ) {
        return correctAnswerKeys.stream().map(String::trim).map(String::toUpperCase)
                .map(key -> options.get(key.charAt(0) - 'A').getContent().trim()).toList();
        //这一步的核心宗旨，还是：存储到题目表中的correctAnswer是 correct-answerContent，而非Key
        //因为如果你只存A/B/C/D，在QuestionInfoServiceImpl中就没办法比较 用户输入 和 正确答案 的判断结果了（万一选项顺序变了呢）
    }
}
