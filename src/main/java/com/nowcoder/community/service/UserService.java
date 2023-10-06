package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class UserService implements CommunityConstant {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    public User findUserById(int id){
        return userMapper.selectById(id);
    }

    /*注册*/
    public Map<String,Object> register(User user){
        Map<String,Object> map=new HashMap<>();

        // 空值处理
        if(user==null){
            throw new IllegalArgumentException("参数不能为空！");
        }
        if (StringUtils.isBlank(user.getUsername())){
            map.put("usernameMsg","账号不能为空！");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())){
            map.put("passwordMsg","密码不能为空！");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())){
            map.put("emailMsg","邮箱不能为空！");
            return map;
        }

        //验证账号
        User u = userMapper.selectByName(user.getUsername());
        if(u!=null){
            map.put("usernameMsg","账号已存在！");
            return map;
        }

        //验证邮箱
        //判断条件：可以通过邮箱查到用户
        u = userMapper.selectByEmail(user.getEmail());
        if(u!=null){
            map.put("emailMsg","邮箱已被注册！");
            return map;
        }
        //验证邮箱
        //判断条件：可以通过邮箱查到激活的用户(自己的)
//        u = userMapper.selectByEmail(user.getEmail());
//        if(u!=null){
//            if(u.getStatus()==1){
//                map.put("emailMsg","邮箱已被注册！");
//                return map;
//            }else {//否则删除用户未激活的用户
//                userMapper.deleteById(u.getId());
//            }
//        }

        // 注册用户
        // 对密码进行加密
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        user.setPassword(CommunityUtil.md5(user.getPassword()+user.getSalt()));
        //设置为普通用户
        user.setType(0);
        //用户未激活
        user.setStatus(0);
        // 随机生成注册码
        user.setActivationCode(CommunityUtil.generateUUID().substring(0,6));
        // 设置一个随机头像
        user.setHeaderUrl(String.format("https://images.nowcoder.com/head/%dt.png",new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        // 插入用户
        userMapper.insertUser(user);

        //用户激活
        //给用户发送激活邮件,用户通过前往邮件发送的网址进行激活
        Context context=new Context();
        context.setVariable("email",user.getEmail());
        //http://localhost:8080/community/activation/101/code
        String url=domain+contextPath+"/activation/"+user.getId()+"/"+user.getActivationCode();
        context.setVariable("url",url);

        String content = templateEngine.process("/mail/activation.html", context);

        mailClient.sendMail(user.getEmail(),"激活账号",content);

        return map;
    }

    /*激活*/
    public int activation(int userId,String code){
        User user = userMapper.selectById(userId);
        if(user.getStatus()==1){
            return ACTIVATION_REPEAT;
        }else if(user.getActivationCode().equals(code)){
            // 把用户状态改为1
            userMapper.updateStatus(userId,1);
            return ACTIVATION_SUCCESS;
        }else{
            return ACTIVATION_FAILURE;
        }
    }

    /*登录*/
    public Map<String,Object> login(String username,String password,int expiredSeconds){
        Map<String,Object> map=new HashMap<>();

        //空值处理
        if (username==null) {
            map.put("usernameMsg","账号不能为空！");
            return map;
        }
        if (password==null) {
            map.put("passwordMsg","密码不能为空！");
            return map;
        }

        //验证账号
        User u = userMapper.selectByName(username);
        if(u==null){
            map.put("usernameMsg","账号不存在！");
            return map;
        }
        //验证状态
        if(u.getStatus()==0){
            map.put("usernameMsg","账号未激活！");
            return map;
        }
        //验证密码
        if(!u.getPassword().equals(CommunityUtil.md5(password+u.getSalt()))){
            map.put("passwordMsg","密码错误！");
            return map;
        }

        //账号（已激活）密码正确
        //生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(u.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis()+1000*expiredSeconds));
        //插入登录凭证
        loginTicketMapper.insertLoginTicket(loginTicket);

        map.put("ticket",loginTicket.getTicket());

        return map;
    }

    /*退出*/
    public void logout(String ticket){
        loginTicketMapper.updateStatus(ticket,1);
    }

    /*查询登录凭证*/
    public LoginTicket findLoginTicket(String ticket){
        return loginTicketMapper.selectByTicket(ticket);
    }

    /*更新用户头像路径*/
    public int updateHeader(int userId,String headerUrl){
        return userMapper.updateHeader(userId,headerUrl);
    }
}
