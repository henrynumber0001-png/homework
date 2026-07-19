package com.homework.web.app.vo;

import com.homework.model.enums.QuestionInfoQuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NoteVO {
    private Long noteId;
    private Long questionId;
    private String noteContent;
    private String title;
    private QuestionInfoQuestionType questionType;
    private List<String> options;
    private String imageUrl;
    @Schema(description = "正确选项")
    private List<String> correctAnswer;
    @Schema(description = "答案解析")
    private String analysis;
    private LocalDateTime updatedTime;
}

/*
功能的设计逻辑：
写笔记的题，不一定非要是做错的题目 或 收藏的题目

因此该功能要独立开发
 */
