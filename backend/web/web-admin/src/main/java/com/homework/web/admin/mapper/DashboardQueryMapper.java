package com.homework.web.admin.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 管理端单页概览聚合查询。 */
public interface DashboardQueryMapper {

    /** 查询某天全部题库浏览量。 */
    @Select("SELECT COALESCE(SUM(view_count), 0) FROM bank_stat_daily WHERE stat_date = #{date} AND is_deleted = 0")
    Long sumDailyBankViews(@Param("date") LocalDate date);

    /** 查询某天单个题库浏览量。 */
    @Select("""
            SELECT COALESCE(SUM(view_count), 0)
            FROM bank_stat_daily
            WHERE stat_date = #{date} AND bank_id = #{bankId} AND is_deleted = 0
            """)
    Long sumDailyBankViewsByBank(@Param("date") LocalDate date, @Param("bankId") Long bankId);

    /** 查询全部题库累计浏览量。 */
    @Select("SELECT COALESCE(SUM(view_count), 0) FROM question_bank WHERE is_deleted = 0")
    Long sumTotalBankViews();

    /** 查询单个题库累计浏览量。 */
    @Select("SELECT COALESCE(view_count, 0) FROM question_bank WHERE id = #{bankId} AND is_deleted = 0")
    Long selectTotalBankViewsByBank(@Param("bankId") Long bankId);

    /** 查询某天全部题库完成人数。 */
    @Select("""
            SELECT COALESCE(SUM(complete_user_count), 0)
            FROM bank_stat_daily WHERE stat_date = #{date} AND is_deleted = 0
            """)
    Long sumDailyBankCompleted(@Param("date") LocalDate date);

    /** 查询某天单个题库完成人数。 */
    @Select("""
            SELECT COALESCE(SUM(complete_user_count), 0)
            FROM bank_stat_daily
            WHERE stat_date = #{date} AND bank_id = #{bankId} AND is_deleted = 0
            """)
    Long sumDailyBankCompletedByBank(@Param("date") LocalDate date, @Param("bankId") Long bankId);

    /** 查询全部题库累计完成数。 */
    @Select("SELECT COALESCE(SUM(complete_count), 0) FROM question_bank WHERE is_deleted = 0")
    Long sumTotalBankCompleted();

    /** 查询单个题库累计完成数。 */
    @Select("SELECT COALESCE(complete_count, 0) FROM question_bank WHERE id = #{bankId} AND is_deleted = 0")
    Long selectTotalBankCompletedByBank(@Param("bankId") Long bankId);

    /** 查询指定日期的通用看板指标。 */
    @Select("""
            SELECT COALESCE(SUM(${column}), 0)
            FROM dashboard_stat_daily
            WHERE stat_date = #{date} AND is_deleted = 0
            """)
    Long selectDailyMetric(@Param("date") LocalDate date, @Param("column") String column);

    /** 查询通用看板指标的历史累计。 */
    @Select("SELECT COALESCE(SUM(${column}), 0) FROM dashboard_stat_daily WHERE is_deleted = 0")
    Long selectTotalMetric(@Param("column") String column);

    /** 查询指定日期统计最近更新时间。 */
    @Select("""
            SELECT MAX(updated_time)
            FROM dashboard_stat_daily
            WHERE stat_date = #{date} AND is_deleted = 0
            """)
    LocalDateTime selectUpdatedTime(@Param("date") LocalDate date);
}
