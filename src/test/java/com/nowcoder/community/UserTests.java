package com.nowcoder.community;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = StudyApplication.class)
public class UserTests {
    @Autowired
    private UserService userService;

    @Test
    public void testFindUserByName(){
        User user = userService.findUserByName("aaa");
        System.out.println(user);
    }

    @Test
    public void testUpdatePassword(){
        userService.updatePassword(151, CommunityUtil.md5("123456"+"f8910"));
    }
}
