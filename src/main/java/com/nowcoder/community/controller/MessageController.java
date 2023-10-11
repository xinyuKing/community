package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MessageController {
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
        return "/site/letter";
    }

    @RequestMapping(path = "/letter/detail/{conversationId}",method = RequestMethod.GET)
    public String getLetterDetail(@PathVariable(name = "conversationId")String conversationId,Model model,Page page){
        //设置分页信息
        page.setLimit(5);
        page.setRows(messageService.findLetterCount(conversationId));
        page.setPath("/letter/detail/"+conversationId);
        //查询私信信息
        List<Message> letterList = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());

        List<Map<String,Object>> letters=new ArrayList<>();
        if (letterList!=null) {
            for (Message message : letterList) {
                Map<String,Object> map=new HashMap<>();
                map.put("letter",message);
                int fromId = message.getFromId();
                map.put("fromUser",userService.findUserById(fromId));
                letters.add(map);
            }
        }

        model.addAttribute("letters",letters);

        //查询私信目标
        model.addAttribute("target",getLetterTarget(conversationId));

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

}
