package com.app.com.grid07.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class NotificationService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public void handleBotNotification(Long userId , String botName){
        String cooldownKey = "notif_cooldown:user_" + userId;
        String pendingKey = "user:" + userId + ":pending_notifs";

        Boolean hasCooldown = redisTemplate.hasKey(cooldownKey);

        if (Boolean.TRUE.equals(hasCooldown)) {
            redisTemplate.opsForList().rightPush(pendingKey, botName + " replied to your post");
        } else {
            System.out.println("Push Notification Sent to User: " + userId);
            redisTemplate.opsForValue().set(cooldownKey, "1", 15, TimeUnit.MINUTES);
        }

    }

    @Scheduled(fixedRate = 300000)
    public void sweepPendingNotifications() {
        var keys = redisTemplate.keys("user:*:pending_notifs");
        if (keys == null || keys.isEmpty()) return;

        for (String key : keys) {
            List<String> pending = redisTemplate.opsForList().range(key, 0, -1);
            if (pending == null || pending.isEmpty()) continue;

            String userId = key.split(":")[1];
            System.out.println("Summarized Push Notification: " + pending.get(0) +
                    " and " + (pending.size() - 1) + " others interacted with your posts.");

            redisTemplate.delete(key);
        }
    }
}
