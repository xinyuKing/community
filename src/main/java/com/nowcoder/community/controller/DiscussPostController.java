package com.nowcoder.community.controller;

import com.nowcoder.community.entity.*;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {
    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(path = "/mypost/{userId}",method = RequestMethod.GET)
    public String mypost(@PathVariable("userId")int userId,Page page,Model model){
        //配置分页信息
        int rows = discussPostService.findDiscussPostRowsByUserId(userId);
        page.setRows(rows);
        page.setPath("/discuss/mypost/"+userId);
        page.setLimit(5);

        model.addAttribute("rows",rows);
        User user = userService.findUserById(userId);
        model.addAttribute("user",user);
        List<DiscussPost> posts = discussPostService.findDiscussPostByUserId(userId, page.getOffset(), page.getLimit());
        List<Map<String,Object>> postVoList=new ArrayList<>();
        for (DiscussPost post : posts) {
            Map<String,Object> postVo=new HashMap<>();
            postVo.put("post",post);
            //点赞数量
            long likeCount = likeService.findEntityLikeCount(LIKE_TYPE_POST, post.getId());
            postVo.put("likeCount",likeCount);
            postVoList.add(postVo);
        }
        model.addAttribute("posts",postVoList);
        return "/site/my-post";
    }

    @RequestMapping(path = "/add",method = RequestMethod.POST)
    @ResponseBody
    public String addDiscussPost(String title,String content){
        // 在Security中已经进行了拦截，这里可以不验证
        User user = hostHolder.getUser();
        if(user==null){
            return CommunityUtil.getJSONString(403,"您还没有登录!");
        }
        DiscussPost discussPost = new DiscussPost();
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setUserId(user.getId());
        discussPost.setCreateTime(new Date());
        discussPostService.addDiscussPost(discussPost);

        //触发发帖事件
        Event event=new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(user.getId())
                //在这里ENTITY_TYPE_POST更好理解一些，前面ENTITY_TYPE_COMMENT更好理解一些，常量设置少了
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(discussPost.getId());
        eventProducer.fireEvent(event);

        //用于计算帖子分数，记录需要刷新分数的帖子id
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey,discussPost.getId());

        //报错的情况将来统一处理
        return CommunityUtil.getJSONString(0,"发布成功！");
    }

    @RequestMapping(path = "/detail/{disPostId}",method = RequestMethod.GET)
    public String getDiscussPost(Model model, @PathVariable(name = "disPostId") int disPostId, Page page){
        //帖子
        DiscussPost post = discussPostService.findDiscussPostById(disPostId);
        model.addAttribute("post",post);
        //作者
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user",user);
        //评论分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/"+disPostId);
        page.setRows(post.getCommentCount());

        //评论：给帖子的评论
        //回复：给评论的评论
        //评论的列表
        List<Comment> comments = commentService.findCommentsByEntity(ENTITY_TYPE_POST, post.getId(), page.getOffset(), page.getLimit());
        //评论的Vo列表
        List<Map<String,Object>> commentVoList=new ArrayList<>();
        //点赞数量
        long likeCount = likeService.findEntityLikeCount(LIKE_TYPE_POST, disPostId);
        model.addAttribute("likeCount",likeCount);
        //点赞状态
        int likeStatus = hostHolder.getUser()==null?0:likeService.findEntityLikeStatus(hostHolder.getUser().getId(), LIKE_TYPE_POST, disPostId);
        model.addAttribute("likeStatus",likeStatus);

        if(comments!=null){
            for (Comment comment : comments) {
                //评论的Vo
                Map<String,Object> commentVo=new HashMap<>();
                commentVo.put("comment",comment);
                //评论的作者
                User user1 = userService.findUserById(comment.getUserId());
                commentVo.put("user",user1);

                //点赞数量
                likeCount = likeService.findEntityLikeCount(LIKE_TYPE_COMMENT, comment.getId());
                commentVo.put("likeCount",likeCount);
                //点赞状态
                likeStatus = hostHolder.getUser()==null?0:likeService.findEntityLikeStatus(hostHolder.getUser().getId(), LIKE_TYPE_COMMENT, comment.getId());
                commentVo.put("likeStatus",likeStatus);

                //回复列表
                List<Comment> replys = commentService.findCommentsByEntity(ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);
                //回复的Vo列表
                List<Map<String,Object>> replyVoList=new ArrayList<>();
                if(replys!=null){
                    for (Comment reply : replys) {
                        //回复的Vo
                        Map<String,Object> replyVo=new HashMap<>();
                        replyVo.put("reply",reply);
                        //回复的作者
                        User user2 = userService.findUserById(reply.getUserId());
                        replyVo.put("user",user2);
                        //回复的目标
                        User target=reply.getTargetId()==0?null:userService.findUserById(reply.getTargetId());
                        replyVo.put("target",target);
                        //点赞数量
                        long likeCount1 = likeService.findEntityLikeCount(LIKE_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeCount",likeCount1);
                        //点赞状态
                        int likeStatus1 = hostHolder.getUser()==null?0:likeService.findEntityLikeStatus(hostHolder.getUser().getId(), LIKE_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeStatus",likeStatus1);

                        replyVoList.add(replyVo);
                    }
                }

                commentVo.put("replys",replyVoList);

                //回复数量
                int replyCount = commentService.findCountByEntity(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("replyCount",replyCount);

                commentVoList.add(commentVo);
            }
        }

        model.addAttribute("comments",commentVoList);

        return "/site/discuss-detail";
    }

    //置顶
    @RequestMapping(path = "/top",method = RequestMethod.POST)
    @ResponseBody
    public String setTop(int id){
        discussPostService.updateType(id,1);

        //同步到elasticsearch中
        //触发发帖事件
        Event event=new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                //在这里ENTITY_TYPE_POST更好理解一些，前面ENTITY_TYPE_COMMENT更好理解一些，常量设置少了
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);
    }

    //加精
    @RequestMapping(path = "/wonderful",method = RequestMethod.POST)
    @ResponseBody
    public String setWonderful(int id){
        discussPostService.updateStatus(id,1);

        //同步到elasticsearch中
        //触发发帖事件
        Event event=new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                //在这里ENTITY_TYPE_POST更好理解一些，前面ENTITY_TYPE_COMMENT更好理解一些，常量设置少了
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        //计算帖子分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey,id);

        return CommunityUtil.getJSONString(0);
    }

    //删除
    @RequestMapping(path = "/delete",method = RequestMethod.POST)
    @ResponseBody
    public String setDelete(int id){
        discussPostService.updateStatus(id,2);

        //同步到elasticsearch中
        //触发删帖事件
        Event event=new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                //在这里ENTITY_TYPE_POST更好理解一些，前面ENTITY_TYPE_COMMENT更好理解一些，常量设置少了
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);
    }
}
