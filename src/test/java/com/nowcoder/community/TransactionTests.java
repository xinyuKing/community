package com.nowcoder.community;

import com.nowcoder.community.service.AlphaService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = StudyApplication.class)
public class TransactionTests {
    @Autowired
    private AlphaService alphaService;

    @Test
    public void testSave1(){
        Object result = alphaService.save1();
        System.out.println(result);
    }

    @Test
    public void testSave2(){
        Object result = alphaService.save2();
        System.out.println(result);
    }
}
