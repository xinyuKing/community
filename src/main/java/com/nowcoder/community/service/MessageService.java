package com.nowcoder.community.service;

import com.nowcoder.community.dao.MessageMapper;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class MessageService {
    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    public List<Message> findConversations(int userId,int offset,int limit){
        return messageMapper.selectConversations(userId, offset, limit);
    }

    public int findConversationCount(int userId){
        return messageMapper.selectConversationCount(userId);
    }

    public List<Message> findLetters(String conversationId, int offset, int limit){
        return messageMapper.selectLetters(conversationId, offset, limit);
    }

    public int findLetterCount(String conversationId){
        return messageMapper.selectLetterCount(conversationId);
    }

    public int findLetterUnreadCount(int userId,String conversationId){
        return messageMapper.selectLetterUnreadCount(userId, conversationId);
    }

    //增加消息
    public int addMessage(Message message){
        //防止html标签转义
        message.setContent(HtmlUtils.htmlEscape(message.getContent()));
        //过滤敏感词
        message.setContent(sensitiveFilter.filter(message.getContent()));
        return messageMapper.insertMessage(message);
    }

    //把消息改变为已读
    public int readMessage(List<Integer> ids){
        return messageMapper.updateStatus(ids, 1);
    }

    ////查询某个主题下最新的通知
    public Message findLatestNotice(int userId,String topic){
        return messageMapper.selectLatestNotice(userId, topic);
    }

    //查询某个主题包含的通知数量
    public int findNoticeCount(int userId,String topic){
        return messageMapper.selectNoticeCount(userId, topic);
    }

    //查询某个主题未读的通知数量
    public int findNoticeUnreadCount(int userId,String topic){
        return messageMapper.selectNoticeUnreadCount(userId, topic);
    }

    //查询某个主题所包含的通知列表
    public List<Message> findNotices(int userId, String topic,int offset,int limit){
        return messageMapper.selectNotices(userId, topic, offset, limit);
    }
}
