package com.app.com.grid07.controller;

import com.app.com.grid07.entity.Comment;
import com.app.com.grid07.service.CommentService;
import com.app.com.grid07.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private PostService postService;

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<?> addCommentToPost(@PathVariable Long postId,
                                                    @RequestBody Comment comment) {
        try {
            comment.setPostId(postId);
            Comment savedComment = commentService.addComment(comment);
            if (comment.getAuthorId() > 1000) {
                postService.increaseVirality(postId, "BOT_REPLY");
            } else {
                postService.increaseVirality(postId, "COMMENT");
            }
            return new ResponseEntity<>(savedComment, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.TOO_MANY_REQUESTS);
        }
    }
}
