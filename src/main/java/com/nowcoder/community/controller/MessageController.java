package com.nowcoder.community.controller;

import com.alibaba.fastjson2.JSONObject;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Controller
public class MessageController implements CommunityConstant {
    @Autowired
    private MessageService messageService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    //私信列表
    @RequestMapping(path = "/letter/list",method = RequestMethod.GET)
    public String getLetterList(Model model, Page page){
        User user = hostHolder.getUser();
        //分页信息
        page.setLimit(5);
        page.setPath("/letter/list");
        page.setRows(messageService.findConversationCount(user.getId()));

        List<Message> conversationList = messageService.findConversations(user.getId(), page.getOffset(), page.getLimit());
        List<Map<String,Object>> conversations=new ArrayList<>();
        for (Message conversation : conversationList) {
            Map<String,Object> map=new HashMap<>();
            map.put("conversation",conversation);
            map.put("letterCount",messageService.findLetterCount(conversation.getConversationId()));
            map.put("unreadCount",messageService.findLetterUnreadCount(user.getId(),conversation.getConversationId()));
            int targetId=user.getId()==conversation.getFromId()?conversation.getToId():conversation.getFromId();
            map.put("target",userService.findUserById(targetId));
            conversations.add(map);
        }

        model.addAttribute("conversations",conversations);

        //查询未读消息数量
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount",letterUnreadCount);
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount",noticeUnreadCount);
        return "/site/letter";
    }

    //私信详情
    @RequestMapping(path = "/letter/detail/{conversationId}",method = RequestMethod.GET)
    public String getLetterDetail(@PathVariable(name = "conversationId")String conversationId,Model model,Page page){
        //设置分页信息
        page.setLimit(5);
        page.setRows(messageService.findLetterCount(conversationId));
        page.setPath("/letter/detail/"+conversationId);
        //查询私信信息
        List<Message> letterList = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());

        List<Map<String,Object>> letters=new ArrayList<>();
        List<Integer> ids=new ArrayList<>();
        if (letterList!=null) {
            for (Message message : letterList) {
                //封装返回的map集合
                Map<String,Object> map=new HashMap<>();
                map.put("letter",message);
                int fromId = message.getFromId();
                map.put("fromUser",userService.findUserById(fromId));
                letters.add(map);
                //把未读的消息id加入到ids
                if(message.getStatus()==0&&message.getToId()==hostHolder.getUser().getId()){
                    ids.add(message.getId());
                }
            }
        }

        model.addAttribute("letters",letters);

        //查询私信目标
        model.addAttribute("target",getLetterTarget(conversationId));

        //改变已读的消息为已读
        if(!ids.isEmpty()){
            messageService.readMessage(ids);
        }

        return "/site/letter-detail";
    }

    private User getLetterTarget(String conversationId){
        String[] ids = conversationId.split("_");
        int id0=Integer.valueOf(ids[0]);
        int id1=Integer.valueOf(ids[1]);

        if(hostHolder.getUser().getId()==id0){
            return userService.findUserById(id1);
        }else {
            return userService.findUserById(id0);
        }
    }

    //发私信
    @RequestMapping(path = "/letter/send",method = RequestMethod.POST)
    @ResponseBody
    public String addMessage(String toName,String content,Model model){
        User target = userService.findUserByName(toName);
        if(target==null){
            return CommunityUtil.getJSONString(1,"目标用户不存在！");
        }

        //封装消息
        Message message = new Message();
        message.setFromId(hostHolder.getUser().getId());
        message.setToId(target.getId());
        message.setCreateTime(new Date());
        message.setStatus(0);
        message.setContent(content);
        if(message.getFromId()<message.getToId()){
            message.setConversationId(message.getFromId()+"_"+message.getToId());
        }else {
            message.setConversationId(message.getToId()+"_"+message.getFromId());
        }
        //插入消息
        messageService.addMessage(message);

        return CommunityUtil.getJSONString(0);
    }

    //系统消息列表
    @RequestMapping(path = "/notice/list",method = RequestMethod.GET)
    public String getNoticeList(Model model){
        User user = hostHolder.getUser();
        //查询评论类的通知
        Message message = messageService.findLatestNotice(user.getId(), TOPIC_COMMENT);
        if (message!=null) {
            Map<String,Object> messageVo=new HashMap<>();
            messageVo.put("message",message);

            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String,Object> data = JSONObject.parseObject(content, HashMap.class);

            messageVo.put("user",userService.findUserById((Integer)data.get("userId")));
            messageVo.put("entityType",data.get("entityType"));
            messageVo.put("entityId",data.get("entityId"));
            messageVo.put("postId",data.get("postId"));

            int count = messageService.findNoticeCount(user.getId(), TOPIC_COMMENT);
            messageVo.put("count",count);

            int unreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_COMMENT);
            messageVo.put("unreadCount",unreadCount);
            model.addAttribute("commentNotice",messageVo);
        }


        //查询点赞类的通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_LIKE);

        if (message!=null) {
            Map<String,Object> messageVo=new HashMap<>();
            messageVo.put("message",message);

            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String,Object> data = JSONObject.parseObject(content, HashMap.class);

            messageVo.put("user",userService.findUserById((Integer)data.get("userId")));
            messageVo.put("entityType",data.get("entityType"));
            messageVo.put("entityId",data.get("entityId"));
            messageVo.put("postId",data.get("postId"));

            int count = messageService.findNoticeCount(user.getId(), TOPIC_LIKE);
            messageVo.put("count",count);

            int unreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_LIKE);
            messageVo.put("unreadCount",unreadCount);

            model.addAttribute("likeNotice",messageVo);
        }


        //查询关注类的通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_FOLLOW);

        if (message!=null) {
            Map<String,Object> messageVo=new HashMap<>();
            messageVo.put("message",message);

            String content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String,Object> data = JSONObject.parseObject(content, HashMap.class);

            messageVo.put("user",userService.findUserById((Integer)data.get("userId")));
            messageVo.put("entityType",data.get("entityType"));
            messageVo.put("entityId",data.get("entityId"));

            int count = messageService.findNoticeCount(user.getId(), TOPIC_FOLLOW);
            messageVo.put("count",count);

            int unreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_FOLLOW);
            messageVo.put("unreadCount",unreadCount);

            model.addAttribute("followNotice",messageVo);
        }

        //查询未读消息数量
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount",letterUnreadCount);
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount",noticeUnreadCount);

        return "/site/notice";
    }

    @RequestMapping(path = "/notice/detail/{topic}",method = RequestMethod.GET)
    public String getNoticeDetail(Model model,Page page,@PathVariable(name = "topic")String topic){
        User user = hostHolder.getUser();
        //设置分页信息
        page.setLimit(5);
        page.setRows(messageService.findNoticeCount(user.getId(),topic));
        page.setPath("/notice/detail/"+topic);

        List<Message> noticeList = messageService.findNotices(user.getId(), topic, page.getOffset(), page.getLimit());
        List<Map<String,Object>> noticeVoList=new ArrayList<>();
        List<Integer> ids=new ArrayList<>();
        if(noticeList!=null){
            for (Message notice : noticeList) {
                Map<String,Object> noticeVo=new HashMap<>();
                //通知
                noticeVo.put("notice",notice);

                //内容
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                Map<String,Object> data = JSONObject.parseObject(content, HashMap.class);
                noticeVo.put("user",userService.findUserById((Integer)data.get("userId")));
                noticeVo.put("entityType",data.get("entityType"));
                noticeVo.put("entityId",data.get("entityId"));
                noticeVo.put("postId",data.get("postId"));

                //通知作者
                noticeVo.put("fromUser",userService.findUserById(notice.getFromId()));//这里查询出来的数据是系统用户
                noticeVoList.add(noticeVo);

                //把未读的消息id加入到ids
                if(notice.getStatus()==0&&notice.getToId()==hostHolder.getUser().getId()){
                    ids.add(notice.getId());
                }
            }
        }
        model.addAttribute("notices",noticeVoList);

        //设置已读
        if(!ids.isEmpty()){
            messageService.readMessage(ids);
        }

        return "/site/notice-detail";
    }
}
