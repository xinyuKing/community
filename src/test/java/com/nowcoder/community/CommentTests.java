package com.nowcoder.community;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.CommentService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = StudyApplication.class)
public class CommentTests {
    @Autowired
    private CommentService commentService;

    @Test
    public void testFindCommentsByEntity(){
        Page page = new Page();
        //评论分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/"+228);

        //评论：给帖子的评论
        //回复：给评论的评论
        //评论的列表
        List<Comment> comments = commentService.findCommentsByEntity(1, 228, page.getOffset(), page.getLimit());
        for (Comment comment : comments) {
            System.out.println(comment);
        }
    }

    @Test
    public void testFindCommentById(){
        Comment comment = commentService.findCommentById(2);
        System.out.println(comment);
    }

    @Test
    public void testFindCommentsByUserId(){
        List<Comment> comments = commentService.findCommentsByUserId(111, 0, 5);
        for (Comment comment : comments) {
            System.out.println(comment);
        }
    }

    @Test
    public void testFindCommentRowsByUserId(){
        int rows = commentService.findCommentRowsByUserId(111);
        System.out.println(rows);
    }
}
