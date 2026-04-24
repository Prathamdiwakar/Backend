package com.app.com.grid07.controller;

import com.app.com.grid07.entity.Post;
import com.app.com.grid07.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PostController {

    @Autowired
    private PostService postService;

    @PostMapping("/posts")
    public ResponseEntity<Post> createPost(@RequestBody Post post){
      Post posts = postService.addPost(post);
      return new ResponseEntity<>(posts ,HttpStatus.CREATED);
    }

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<String> addCommentToPost(@PathVariable Long postId) {
        postService.increaseVirality(postId ,"LIKE");
        return new ResponseEntity<>("Like a Post", HttpStatus.OK);
    }
}


