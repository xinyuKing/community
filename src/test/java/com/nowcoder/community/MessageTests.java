package com.nowcoder.community;

import com.nowcoder.community.dao.MessageMapper;
import com.nowcoder.community.entity.Message;
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
public class MessageTests {
    @Autowired
    private MessageMapper messageMapper;

    @Test
    public void testSelectConversation(){
        List<Message> messageList = messageMapper.selectConversations(111, 0, 5);
        for (Message message : messageList) {
            System.out.println(message);
        }
    }

    @Test
    public void testSelectConversationCount(){
        int conversationCount = messageMapper.selectConversationCount(111);
        System.out.println(conversationCount);
    }

    @Test
    public void testLetters(){
        List<Message> letterList = messageMapper.selectLetters("111_112", 0, 5);
        for (Message letter : letterList) {
            System.out.println(letter);
        }
    }

    @Test
    public void testLetterCount(){
        int letterCount = messageMapper.selectLetterCount("111_112");
        System.out.println(letterCount);
    }

    @Test
    public void testLetterUnreadCount(){
        int unreadCount = messageMapper.selectLetterUnreadCount(111, null);
        System.out.println(unreadCount);
    }


}
