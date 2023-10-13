package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
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

    @RequestMapping(path = "/add",method = RequestMethod.POST)
    @ResponseBody
    public String addDiscussPost(String title,String content){
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
        List<Comment> comments = commentService.findCommentsByEntity(ENTITY_TYPE_COMMENT, post.getId(), page.getOffset(), page.getLimit());
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
                List<Comment> replys = commentService.findCommentsByEntity(ENTITY_TYPE_REPLY, comment.getId(), 0, Integer.MAX_VALUE);
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
                int replyCount = commentService.findCountByEntity(ENTITY_TYPE_REPLY, comment.getId());
                commentVo.put("replyCount",replyCount);

                commentVoList.add(commentVo);
            }
        }

        model.addAttribute("comments",commentVoList);

        return "/site/discuss-detail";
    }


}
