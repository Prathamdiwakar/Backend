package com.app.com.grid07.service;

import com.app.com.grid07.entity.Comment;
import com.app.com.grid07.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class CommentServiceImpl implements CommentService{

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private NotificationService notificationService;

    private static final RedisScript<Long> ATOMIC_BOT_INCREMENT = RedisScript.of(
            "local count = redis.call('INCR', KEYS[1]) " +
                    "if count > 100 then " +
                    "  redis.call('DECR', KEYS[1]) " +
                    "  return -1 " +
                    "end " +
                    "return count",
            Long.class
    );

    @Override
    public Comment addComment(Comment comment) {
       if(comment.getDepthLevel()>20){
           throw new RuntimeException("Vertical Cap exceeded:  max depth is 20 ");
       }
       boolean isBot = comment.getAuthorId()>1000;

        if (isBot) {
            String botCountKey = "post:" + comment.getPostId() + ":bot_count";
            Long result = redisTemplate.execute(
                    ATOMIC_BOT_INCREMENT,
                    java.util.Collections.singletonList(botCountKey)
            );
            if (result != null && result == -1L) {
                throw new RuntimeException("Horizontal Cap exceeded: max 100 bot replies");
            }
        }

        if(isBot){
            String coolDownKey = "cooldown:bot"+comment.getAuthorId()+ ":human_" +comment.getAuthorId();
            Boolean exists = redisTemplate.hasKey(coolDownKey);
            if(Boolean.TRUE.equals(exists)){
                throw new RuntimeException("Cooldown Active : bot cannot interact for 10 minutes");
            }
            redisTemplate.opsForValue().set(coolDownKey,"1",10, TimeUnit.MINUTES);
        }

        Comment savedComment = commentRepository.save(comment);


        if(isBot){
            notificationService.handleBotNotification(comment.getAuthorId(), "Bot_" + comment.getAuthorId());
        }
        return savedComment;
    }
}
