package cn.beinet.business.login.dal;

import cn.beinet.business.login.dal.entity.AuditLogs;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 审计日志 Mapper 接口
 * 
 * @author youbl
 * @since 2024-12-20
 */
@Mapper
public interface AuditLogsMapper extends BaseMapper<AuditLogs> {

    /**
     * 分页查询审计日志（带用户名）
     * 
     * @param page 分页参数
     * @param userId 用户ID（可选）
     * @param eventType 事件类型（可选）
     * @param result 结果（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param ipAddress IP地址（可选）
     * @return 分页结果
     */
    @Select({
        "<script>",
        "SELECT a.*, u.name as username FROM audit_logs a",
        "LEFT JOIN users u ON a.user_id = u.id",
        "WHERE 1=1",
        "<if test='userId != null'>AND a.user_id = #{userId}</if>",
        "<if test='eventType != null and eventType != \"\"'>AND a.event_type = #{eventType}</if>",
        "<if test='result != null'>AND a.result = #{result}</if>",
        "<if test='startTime != null'>AND a.create_time >= #{startTime}</if>",
        "<if test='endTime != null'>AND a.create_time <= #{endTime}</if>",
        "<if test='ipAddress != null and ipAddress != \"\"'>AND a.ip_address = #{ipAddress}</if>",
        "ORDER BY a.create_time DESC",
        "</script>"
    })
    IPage<Map<String, Object>> selectAuditLogsWithUsername(
        Page<Map<String, Object>> page,
        @Param("userId") Long userId,
        @Param("eventType") String eventType,
        @Param("result") Integer result,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("ipAddress") String ipAddress
    );

    /**
     * 统计指定天数内的事件类型分布
     * 
     * @param days 天数
     * @return 统计结果
     */
    @Select({
        "SELECT event_type, COUNT(*) as count,",
        "SUM(CASE WHEN result = 1 THEN 1 ELSE 0 END) as success_count,",
        "SUM(CASE WHEN result = 0 THEN 1 ELSE 0 END) as failed_count",
        "FROM audit_logs",
        "WHERE create_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY)",
        "GROUP BY event_type",
        "ORDER BY count DESC"
    })
    List<Map<String, Object>> selectEventTypeStatistics(@Param("days") int days);

    /**
     * 统计指定天数内的每日事件数量
     * 
     * @param days 天数
     * @return 统计结果
     */
    @Select({
        "SELECT DATE(create_time) as date,",
        "COUNT(*) as total_count,",
        "SUM(CASE WHEN result = 1 THEN 1 ELSE 0 END) as success_count,",
        "SUM(CASE WHEN result = 0 THEN 1 ELSE 0 END) as failed_count",
        "FROM audit_logs",
        "WHERE create_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY)",
        "GROUP BY DATE(create_time)",
        "ORDER BY date DESC"
    })
    List<Map<String, Object>> selectDailyStatistics(@Param("days") int days);

    /**
     * 获取指定天数内的总体统计
     * 
     * @param days 天数
     * @return 统计结果
     */
    @Select({
        "SELECT COUNT(*) as total_events,",
        "SUM(CASE WHEN result = 1 THEN 1 ELSE 0 END) as success_events,",
        "SUM(CASE WHEN result = 0 THEN 1 ELSE 0 END) as failed_events",
        "FROM audit_logs",
        "WHERE create_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY)"
    })
    Map<String, Object> selectOverallStatistics(@Param("days") int days);

    /**
     * 清理指定天数之前的审计日志
     * 
     * @param days 保留天数
     * @return 删除的记录数
     */
    @Select("DELETE FROM audit_logs WHERE create_time < DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int cleanupOldLogs(@Param("days") int days);
}