package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.FollowService;
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

import java.util.List;
import java.util.Map;

@Controller
public class FollowController implements CommunityConstant {
    @Autowired
    private FollowService followService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @Autowired
    private EventProducer eventProducer;

    @LoginRequired
    @RequestMapping(path = "/follow",method = RequestMethod.POST)
    @ResponseBody
    public String follow(int entityType,int entityId){
        User user = hostHolder.getUser();

        followService.follow(user.getId(),entityType,entityId);

        //触发关注事件
        Event event = new Event();
        event.setTopic(TOPIC_FOLLOW);
        event.setEntityId(entityId);
        event.setEntityType(entityType);
        event.setUserId(hostHolder.getUser().getId());
        event.setEntityUserId(entityId);

        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0,"已关注！");
    }

    @LoginRequired
    @RequestMapping(path = "/unfollow",method = RequestMethod.POST)
    @ResponseBody
    public String unfollow(int entityType,int entityId){
        User user = hostHolder.getUser();

        followService.unfollow(user.getId(),entityType,entityId);

        return CommunityUtil.getJSONString(0,"已取消关注！");
    }

    @LoginRequired
    @RequestMapping(path = "/followees/{userId}",method = RequestMethod.GET)
    public String followee(Model model, Page page, @PathVariable(name = "userId")int userId){
        User user = userService.findUserById(userId);
        if (user==null){
            throw new IllegalArgumentException("用户不存在");
        }
        model.addAttribute("user",user);
        //设置分页
        page.setLimit(5);
        page.setRows((int) followService.findFolloweeCount(userId,ENTITY_TYPE_USER));
        page.setPath("/followees/"+userId);
        //查询
        List<Map<String, Object>> userList = followService.findFollowees(userId, page.getOffset(), page.getLimit());

        //查询登录用户是否关注该用户关注的实体
        if (userList!=null){
            for (Map<String, Object> map : userList) {
                User u=(User)map.get("user");
                map.put("hasFollowed",hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users",userList);

        return "/site/followee";
    }

    @LoginRequired
    @RequestMapping(path = "/followers/{entityId}",method = RequestMethod.GET)
    public String follower(Model model, Page page, @PathVariable(name = "entityId")int entityId){
        User user = userService.findUserById(entityId);
        if (user==null){
            throw new IllegalArgumentException("该实体不存在");
        }
        model.addAttribute("user",user);
        //设置分页
        page.setLimit(5);
        page.setRows((int) followService.findFollowerCount(ENTITY_TYPE_USER,entityId));
        page.setPath("/followers/"+entityId);
        //查询
        List<Map<String, Object>> userList = followService.findFollowers(entityId, page.getOffset(), page.getLimit());

        //查询登录用户是否关注该用户的粉丝
        if (userList!=null){
            for (Map<String, Object> map : userList) {
                User u=(User)map.get("user");
                map.put("hasFollowed",hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users",userList);

        return "/site/follower";
    }


    //查询登录用户是否关注该用户
    private boolean hasFollowed(int userId){
        //这个判断应该是多余的，因为followee方法加了自定义注解@LoginRequired
        if (hostHolder.getUser()==null){
            return false;
        }
        return followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
    }

}
