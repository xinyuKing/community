package com.nowcoder.community;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = StudyApplication.class)
public class MapperTests {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Test
    public void testSelectById(){
        User user = userMapper.selectById(11);
        System.out.println(user);
    }

    @Test
    public void testSelectByName(){
        User user = userMapper.selectByName("liubei");
        System.out.println(user);
    }

    @Test
    public void testSelectByEmail(){
        User user = userMapper.selectByEmail("nowcoder111@sina.com");
        System.out.println(user);
    }

    @Test
    public void testInsertUser(){
        User user = new User();
        user.setUsername("tom01");
        user.setPassword("21345678");
        user.setSalt("abc");
        user.setEmail("213454632@qq.com");
        user.setHeaderUrl("http://images.nowcoder.com/head/495t.png");
        user.setCreateTime(new Date());

        int rows = userMapper.insertUser(user);
        System.out.println(rows);
        System.out.println(user.getId());
    }

    @Test
    public void testUpdata(){
        userMapper.updateStatus(150,1);
        userMapper.updateHeader(150,"http://images.nowcoder.com/head/496t.png");
        userMapper.updatePassword(150,"431413432");
    }



    @Test
    public void testSelectDiscussPosts(){
        List<DiscussPost> discussPosts = discussPostMapper.selectDiscussPosts(103, 0, 500);
        for (DiscussPost post : discussPosts) {
            System.out.println(post);
        }

        int rows = discussPostMapper.selectDiscussPostRows(103);
        System.out.println(rows);
    }
}
