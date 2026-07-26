package com.homework.web.app.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class LearningCalendarItemVO { //创建日历

    /** 日期。 */
    private LocalDate date;

    /** 当天学习分钟数，用于控制单元格颜色。 */
    private long studyMinutes;
}
