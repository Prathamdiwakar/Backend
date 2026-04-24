package com.app.com.grid07.service;

import com.app.com.grid07.entity.Post;

public interface PostService {
    Post addPost(Post post);
    void increaseVirality(Long postId, String interactionType);
}
