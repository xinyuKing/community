package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {
    private static final Logger logger= LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domian;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Value("${qiniu.key.access}")
    private String access;

    @Value("${qiniu.key.secret}")
    private String secret;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @Value("${qiniu.bucket.header.url}")
    private String headerBucketUrl;

//    优化后自定义的LoginRequired拦截器被弃用
//    @LoginRequired
    @RequestMapping(path = "/setting",method = RequestMethod.GET)
    public String getSettingPage(Model model){
        //生成上传文件的名称
        String fileName = CommunityUtil.generateUUID();
        //设置相应信息
        StringMap policy = new StringMap();
        policy.put("returnBody",CommunityUtil.getJSONString(0));
        // 生成上传凭证
        Auth auth = Auth.create(access, secret);
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy);

        model.addAttribute("uploadToken",uploadToken);
        model.addAttribute("fileName",fileName);
        return "/site/setting";
    }

    //更新头像路径
    @RequestMapping(path = "/header/url",method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(String fileName){
        if (StringUtils.isBlank(fileName)){
            return CommunityUtil.getJSONString(1,"文件名不能为空");
        }

        String url=headerBucketUrl+"/"+fileName;

        userService.updateHeader(hostHolder.getUser().getId(),url);
        return CommunityUtil.getJSONString(0);
    }

    /*// 整个方法被弃用*/
    //    优化后自定义的LoginRequired拦截器被弃用
    @LoginRequired
    @RequestMapping(path = "/upload",method = RequestMethod.POST)
    public String upload(MultipartFile headerImage, Model model){
        if(headerImage==null){
            model.addAttribute("error","您还没有选择图片!");
            return "/site/setting";
        }

        String filename = headerImage.getOriginalFilename();
        String suffix = filename.substring(filename.lastIndexOf("."));
        if(StringUtils.isBlank(suffix)){
            model.addAttribute("error","文件格式错误！!");
            return "/site/setting";
        }

        //生成随机文件名(感觉优点不对劲)
        filename=CommunityUtil.generateUUID()+suffix;
        //确定文件存放路径
        File dest = new File(uploadPath+"/"+filename);
        try {
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传图片错误"+e.getMessage());
            throw new RuntimeException("上传图片错误，服务器运行异常!",e);
        }

        //跟新当前用户的头像路径(web访问路径)
        //http://localhost:8080:/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String url=domian+contextPath+"/user/header/"+filename;
        userService.updateHeader(user.getId(),url);

        return "redirect:/index";
    }

    /*// 整个方法被弃用*/
    @RequestMapping(path = "/header/{filename}",method = RequestMethod.GET)
    public void getFile(@PathVariable("filename") String filename, HttpServletResponse response){
        //服务器存放路径
        filename=uploadPath+"/"+filename;
        //获取文件后缀
        String suffix = filename.substring(filename.lastIndexOf("."));
        //响应图片
        response.setContentType("image/"+suffix);
        try(
                //不需要自己释放
                FileInputStream fis = new FileInputStream(filename);
                ServletOutputStream os = response.getOutputStream();
                ) {
            //读取图片并输出
            byte[] buffer = new byte[1024];
            int b=0;
            while ((b=fis.read(buffer))!=-1){
                os.write(buffer,0,b);
            }
        } catch (IOException e) {
            logger.error("图片读取错误！"+e.getMessage());
        }
    }

    //个人主页
    //    优化后自定义的LoginRequired拦截器被弃用
//    @LoginRequired
    @RequestMapping(path = "/profile/{userId}",method = RequestMethod.GET)
    public String getProfilePage(Model model,@PathVariable(name = "userId")int userId){
        //查询用户
        User user = userService.findUserById(userId);
        if(user==null){
            throw new IllegalArgumentException("用户不存在!");
        }
        model.addAttribute("user",user);
        //获得的点赞数量
        int userLikeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("userLikeCount",userLikeCount);

        //用户关注的实体数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount",followeeCount);

        //关注用户的实体数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount",followerCount);

        //用户是否关注该用户
        boolean hasFollowed = false;
        if(hostHolder.getUser()!=null){
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed",hasFollowed);

        return "/site/profile";
    }
}
