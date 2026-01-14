package com.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notification.entity.NotificationTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知任务 Mapper 接口
 * 
 * @author Notification System
 */
@Mapper
public interface NotificationTaskMapper extends BaseMapper<NotificationTask> {
    
    /**
     * 根据 eventId 和状态列表查询任务
     * 用于幂等性检查
     * 
     * @param eventId 业务事件ID
     * @param statuses 状态列表
     * @return 匹配的任务，如果不存在返回null
     */
    @Select("<script>" +
            "SELECT * FROM notification_task " +
            "WHERE event_id = #{eventId} " +
            "AND status IN " +
            "<foreach collection='statuses' item='status' open='(' separator=',' close=')'>" +
            "#{status}" +
            "</foreach>" +
            " LIMIT 1" +
            "</script>")
    NotificationTask selectByEventIdAndStatusIn(
            @Param("eventId") String eventId,
            @Param("statuses") List<String> statuses
    );
    
    /**
     * 查询指定状态且创建时间早于指定时间的任务
     * 用于数据清理
     * 
     * @param status 任务状态
     * @param before 时间阈值
     * @return 符合条件的任务列表
     */
    @Select("SELECT * FROM notification_task " +
            "WHERE status = #{status} " +
            "AND created_at < #{before}")
    List<NotificationTask> selectByStatusAndCreatedAtBefore(
            @Param("status") String status,
            @Param("before") LocalDateTime before
    );
    
    /**
     * 批量删除任务
     * 
     * @param ids 任务ID列表
     * @return 删除的记录数
     */
    int deleteBatchByIds(@Param("ids") List<String> ids);
}
