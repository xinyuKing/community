package com.nowcoder.community.controller;

import com.nowcoder.community.entity.*;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.*;

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
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(path = "/myreply/{userId}",method = RequestMethod.GET)
    public String myreply(@PathVariable(name = "userId")int userId, Page page, Model model){
        int rows = commentService.findCommentRowsByUserId(userId);
        //设置分页信息
        page.setRows(rows);
        page.setLimit(5);
        page.setPath("/comment/myreply/"+userId);

        User user = userService.findUserById(userId);
        model.addAttribute("user",user);

        List<Comment> commentsList = commentService.findCommentsByUserId(userId,page.getOffset(),page.getLimit());

        List<Map<String,Object>> commentVoList=new ArrayList<>();
        for (Comment comment : commentsList) {
            Map<String,Object> commentVo=new HashMap<>();
            commentVo.put("comment",comment);
            //如果是回复，找到回复是在哪个回帖下
            while (comment.getEntityType()==2){
                comment=commentService.findCommentById(comment.getEntityId());
            }
            DiscussPost post = discussPostService.findDiscussPostById(comment.getEntityId());
            commentVo.put("post",post);
            commentVoList.add(commentVo);
        }
        model.addAttribute("comments",commentVoList);
        return "/site/my-reply";
    }

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

        if(comment.getEntityType()==ENTITY_TYPE_POST){
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }else if(comment.getEntityType()==ENTITY_TYPE_COMMENT){
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        eventProducer.fireEvent(event);
        //修改ES中帖子的回帖数量
        if(comment.getEntityType()==ENTITY_TYPE_POST){
            event = new Event();
            event.setTopic(TOPIC_PUBLISH);
            //在这里ENTITY_TYPE_POST更好理解一些，前面ENTITY_TYPE_COMMENT更好理解一些，常量设置少了
            event.setEntityType(ENTITY_TYPE_POST);
            event.setEntityId(discussPostId);
            event.setUserId(hostHolder.getUser().getId());
            //计算帖子分数
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey,discussPostId);
        }

        return "redirect:/discuss/detail/"+discussPostId;
    }
}
