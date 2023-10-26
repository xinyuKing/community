package com.nowcoder.community;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.DiscussPostService;
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
public class DiscussPostTests {

    @Autowired
    private DiscussPostService discussPostService;

    @Test
    public void testFindPostByUserId(){
        List<DiscussPost> posts = discussPostService.findDiscussPostByUserId(111, 1, 5);
        for (DiscussPost post : posts) {
            System.out.println(post);
        }
    }

    @Test
    public void testFindPostRowsByUserId(){
        int rows = discussPostService.findDiscussPostRowsByUserId(111);
        System.out.println(rows);
    }
}
