package com.app.com.grid07.service;

import com.app.com.grid07.entity.Post;
import com.app.com.grid07.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PostServiceImpl implements PostService{

    @Autowired
    PostRepository postRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public Post addPost(Post post) {
        return postRepository.save(post);
    }

    public void increaseVirality(Long postId, String interactionType){
        String key = "post:" + postId + ":virality_score";
        int value = 0;
        if ("LIKE".equalsIgnoreCase(interactionType)) {
            value = 20;
        } else if ("COMMENT".equalsIgnoreCase(interactionType)) {
            value = 50;
        } else if ("BOT_REPLY".equalsIgnoreCase(interactionType)) {
            value = 1;
        }

        redisTemplate.opsForValue().increment(key, value);
    }
}
