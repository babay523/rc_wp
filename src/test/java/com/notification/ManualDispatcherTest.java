package com.notification;

import com.notification.service.NotificationDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 手动测试 Dispatcher（不依赖 RocketMQ）
 * 
 * 运行此测试可以验证完整的投递流程
 */
@SpringBootTest(classes = NotificationSystemApplication.class)
@ActiveProfiles("test")
public class ManualDispatcherTest {
    
    @Autowired
    private NotificationDispatcher dispatcher;
    
    /**
     * 测试: 手动触发 dispatcher
     * 
     * 步骤:
     * 1. 先通过 API 创建一个通知任务
     * 2. 获取 notificationId
     * 3. 手动调用 dispatcher.dispatch(notificationId)
     * 4. 查看任务状态是否更新
     */
    @Test
    public void testManualDispatch() {
        // 这是一个示例测试
        // 实际使用时，需要先创建任务，然后传入 notificationId
        
        String notificationId = "ntf_20260114000000_test";
        
        System.out.println("========================================");
        System.out.println("手动测试 Dispatcher");
        System.out.println("========================================");
        System.out.println();
        System.out.println("使用步骤:");
        System.out.println("1. 启动应用（不需要 RocketMQ）");
        System.out.println("2. 通过 API 创建通知任务");
        System.out.println("3. 复制返回的 notificationId");
        System.out.println("4. 修改此测试中的 notificationId");
        System.out.println("5. 运行此测试");
        System.out.println();
        System.out.println("这样可以在没有 RocketMQ 的情况下测试完整的投递流程");
        System.out.println();
        
        // 取消注释以下代码来实际测试
        // dispatcher.dispatch(notificationId);
    }
}
