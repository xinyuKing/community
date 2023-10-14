package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

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

    //优化后存在redis中
/*    @Autowired
    private LoginTicketMapper loginTicketMapper;*/

    @Autowired
    private RedisTemplate redisTemplate;

    public User findUserById(int id){
        //优化后先从redis中查询
//        return userMapper.selectById(id);
        User user = getCache(id);
        if(user!=null){
            return user;
        }else {
            return initCache(id);
        }
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
        //这一步调用该类中优化后的方法findUserById和直接读那个更好一些???????
        User user = findUserById(userId);
//        User user = userMapper.selectById(userId);
        if(user.getStatus()==1){
            return ACTIVATION_REPEAT;
        }else if(user.getActivationCode().equals(code)){
            // 把用户状态改为1
            userMapper.updateStatus(userId,1);
            //清除redis中的该用户
            clearCache(userId);
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
        //插入登录凭证，优化后存在redis中
//        loginTicketMapper.insertLoginTicket(loginTicket);

        String ticketKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        //redis会自动把对象序列化成json字符串
        //别忘记设置存活时间
        redisTemplate.opsForValue().set(ticketKey,loginTicket,expiredSeconds,TimeUnit.SECONDS);

        map.put("ticket",loginTicket.getTicket());

        return map;
    }

    /*退出*/
    public void logout(String ticket){
        //已经把登录凭证存入Redis中
//        loginTicketMapper.updateStatus(ticket,1);
        //从redis中读出登录凭证，修改状态后再存入
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket=(LoginTicket)redisTemplate.opsForValue().get(ticketKey);
        loginTicket.setStatus(1);//修改登录状态
        redisTemplate.opsForValue().set(ticketKey,loginTicket);
    }

    /*查询登录凭证*/
    public LoginTicket findLoginTicket(String ticket){
//        return loginTicketMapper.selectByTicket(ticket);
        //优化后把登录凭证存入了Redis中
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket)redisTemplate.opsForValue().get(ticketKey);
    }

    /*更新用户头像路径*/
    public int updateHeader(int userId,String headerUrl){
//        return userMapper.updateHeader(userId,headerUrl);
        int rows = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return rows;
    }

    /*通过用户姓名寻找用户*/
    public User findUserByName(String username){
        return userMapper.selectByName(username);
    }

    //优先从缓存中取值
    public User getCache(int userId){
        String userKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(userKey);
    }

    //取不到时初始化缓存数据
    public User initCache(int userId){
        //从数据表中读取user
        User user = userMapper.selectById(userId);
        //把user存入redis中
        String userKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(userKey,user,3600, TimeUnit.SECONDS);
        return user;
    }

    //数据变更时清除缓存数据
    public void clearCache(int userId){
        String userKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(userKey);
    }
}
