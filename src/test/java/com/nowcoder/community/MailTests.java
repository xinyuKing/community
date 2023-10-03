package com.nowcoder.community;

import com.nowcoder.community.util.MailClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = StudyApplication.class)
public class MailTests {
    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Test
    public void testTextMail(){
        mailClient.sendMail("jin_xinyu@163.com","TEST","Hello mail!");
    }

    @Test
    public void testHtmlMail(){
        Context context=new Context();
        context.setVariable("username","tom");

        String content = templateEngine.process("/mail/demo.html", context);
        System.out.println(content);

        mailClient.sendMail("jin_xinyu@163.com","HTML",content);
    }
}
