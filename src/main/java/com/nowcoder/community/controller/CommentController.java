package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {
    @Autowired
    private CommentService commentService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(path = "/add/{discussPostId}",method = RequestMethod.POST)
    public String addComment(@PathVariable(name = "discussPostId")int discussPostId, Comment comment){
        comment.setCreateTime(new Date(System.currentTimeMillis()));
        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);

        commentService.addComment(comment);
        //触发评论事件
        Event event = new Event();
        event.setTopic(TOPIC_COMMENT);
        event.setEntityType(comment.getEntityType());
        event.setEntityId(comment.getEntityId());
        event.setUserId(hostHolder.getUser().getId());
        event.setData("postId",discussPostId);

        if(comment.getEntityType()==ENTITY_TYPE_COMMENT){
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }else if(comment.getEntityType()==ENTITY_TYPE_REPLY){
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        eventProducer.fireEvent(event);
        //修改ES中帖子的回帖数量
        if(comment.getEntityType()==ENTITY_TYPE_COMMENT){
            event = new Event();
            event.setTopic(TOPIC_PUBLISH);
            //在这里ENTITY_TYPE_POST更好理解一些，前面ENTITY_TYPE_COMMENT更好理解一些，常量设置少了
            event.setEntityType(ENTITY_TYPE_COMMENT);
            event.setEntityId(discussPostId);
            event.setUserId(hostHolder.getUser().getId());
            //计算帖子分数
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey,discussPostId);
        }

        return "redirect:/discuss/detail/"+discussPostId;
    }
}
