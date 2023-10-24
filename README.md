## 工具类

MailClient:发送邮件（只需要输入对方邮箱，标题和内容就可以发送）

CommunityUtil:生成随机字符串，md5加密，生成返回的json字符串（通过不同的参数重载成3个）

RedisKeyUtil:生成reids的key

HostHolder:持有用户信息，用于代替用户对象

SensitiveFilter:敏感词过滤，创建前缀树的内部生成类，读取敏感词的txt文档，生成前缀树对象，通过生成的前缀树过滤敏感词

CommunityConstant:定义常数的接口



## 首页

```java
@RequestMapping(path = "/index",method = RequestMethod.GET)
```

get请求分页展示的帖子（有最新和最热两种）：创建Page类（当前页码，一页的上限，数据的总行数（用于计算总的页数），查询路径（用于复用分页的链接））,通过Page计算出的offset和limit查询出帖子列表（查询帖子分为两种，最新：直接查询数据库；最热：DiscussPostService的Bean实例化后执行init()初始化帖子列表缓存，从数据库中读取最热的帖子，把这些帖子保存到Caffeine，因为最热帖子是通过score（打分依靠自己的评判标准）进行排名的，而这些分数会产生变化，所以我们可以使用quartz对帖子进行定期的刷新,例如:我们5分钟刷新一次），

```java
//刷新帖子分数任务
@Bean
public JobDetailFactoryBean postScoreRefreshJobDetail(){
    JobDetailFactoryBean factoryBean=new JobDetailFactoryBean();
    factoryBean.setJobClass(PostScoreRefreshJob.class);
    factoryBean.setName("postScoreRefreshJob");
    factoryBean.setGroup("postScoreRefreshJobGroup");
    factoryBean.setDurability(true);
    factoryBean.setRequestsRecovery(true);
    return factoryBean;
}

@Bean
public SimpleTriggerFactoryBean postScoreRefreshTrigger(JobDetail postScoreRefreshJobDetail){
    SimpleTriggerFactoryBean factoryBean=new SimpleTriggerFactoryBean();
    factoryBean.setJobDetail(postScoreRefreshJobDetail);
    factoryBean.setName("postScoreRefreshTrigger");
    factoryBean.setGroup("communityTriggerGroup");
    factoryBean.setRepeatInterval(1000*60*5);
    factoryBean.setJobDataMap(new JobDataMap());
    return factoryBean;
}
```

再通过帖子列表中的每个帖子查询用户和从redis中查询点赞数量，封装成map，再把map加到list中，返回给前端。

分页按钮功能实现：

```html
<!-- 分页 -->
<nav class="mt-5" th:if="${page.rows>0}" th:fragment="pagination">
   <ul class="pagination justify-content-center">
      <li class="page-item">
         <a class="page-link" th:href="@{${page.path}(current=1)}">首页</a>
      </li>
      <li th:class="|page-item ${page.current==1?'disabled':''}|">
         <a class="page-link" th:href="@{${page.path}(current=${page.current-1})}">上一页</a>
      </li>
      <li th:class="|page-item ${page.current==i?'active':''}|" th:each="i:${#numbers.sequence(page.from,page.to)}">
         <a class="page-link" th:href="@{${page.path}(current=${i})}" th:utext="${i}">1</a>
      </li>
      <li th:class="|page-item ${page.current==page.total?'disabled':''}|">
         <a class="page-link" th:href="@{${page.path}(current=${page.current+1})}">下一页</a>
      </li>
      <li class="page-item">
         <a class="page-link" th:href="@{${page.path}(current=${page.total})}">末页</a>
      </li>
   </ul>
</nav>
```



## 注册

```java
@RequestMapping(path = "/register",method = RequestMethod.GET)
```

get返回注册页面

```java
@RequestMapping(path = "/register",method = RequestMethod.POST)
```

post提交注册数据

检查提交数据是否合理

通过JavaMailSender实现邮件发送工具类，发送激活邮件，邮件中包含激活链接（localhost:8080/community/{userId}/{code},发送邮件成功跳转到中转页面，失败跳转到注册页面）

用户激活，在service中，从数据库中读取该用户，并存入redis中（激活成功通过中转页面跳转到登录页面，重复激活通过中转页面跳转到主页，激活失败通过中转页面跳转到注册页面

**再次注册时可能有问题，在注册时要判断该邮箱是否存在已激活的用户，若存在未激活的用户，要先删除再重新注册**



## 登录

```java
@RequestMapping(path = "/login",method = RequestMethod.GET)
```

get返回登录页面

```java
@RequestMapping(path = "/kaptcha",method = RequestMethod.GET)
```

通过kaptcha（配置参数写在KaptchaConfig中）生成验证码文本和验证码图片，生成验证码归属（可以通过自己编写的RedisKeyUtil生成redis的key）并存入cookie,将验证码文本并存入redis（并设置其生存时间，例如60s），验证码图片传给浏览器。

```java
@RequestMapping(path = "/login",method = RequestMethod.POST)
```

post提交登录数据

controller层从cookie中取出验证码归属，在通过自己编写的RedisKeyUtil生成redis的key，从redis中取出验证码文本和浏览器提交的验证码做对比。

controller层将用户名，密码和登录凭证过期时间传给service层，service层验证账号密码（错误时返回封装了Msg的map）后，生成登录凭证（用登录凭证中的ticket生成redis的key）并存入redis中（redis会自动把对象序列化成json字符串，将登录凭证过期时间设置为生存时间）

主页的登录状态通过SpringSecurity实现，设置过滤器LoginTicketInterceptor不过滤静态资源，在LoginTicketInterceptor中的preHandle中先从cookie获取ticket，再通过ticket从redis中查询loginTicket，如果loginTicket有效，根据loginTic中的userId查询出用户（如果redis中有，直接从redis中取，否则从数据中取，并保存到redis中）。将用户存入hostHolder中，最后构建用户认证的结果，并存入SecurityContext，以便于SpringSecurity进行授权。在SpringSecurity中授权配置中设置一些URL访问需要的权限，分为四种，

```java
.antMatchers(
        "/user/setting",
        "/user/upload",
        "/discuss/add",
        "/comment/add/**",
        "/letter/**",
        "/notice/**",
        "/like",
        "/follow",
        "/unfollow",
        "/profile/**"
).
hasAnyAuthority(
        AUTHORITY_USER, //普通用户
        AUTHORITY_ADMIN, //管理员
        AUTHORITY_MODERATOR //版主
)
.antMatchers(
        "/discuss/top",
        "/discuss/wonderful"
).
hasAnyAuthority(
        AUTHORITY_MODERATOR
)
.antMatchers(
        "/discuss/delete",
        "/data/**",
        "/actuator/**"
).
hasAnyAuthority(
        AUTHORITY_ADMIN
)
.anyRequest().permitAll() // 其他路径未登录用户也可以访问
```

再对权限不足进行处理（分为未有登录和权限不足，这这两种情况还要判断是AJAX请求还是非AJAX请求）

```java
.authenticationEntryPoint(new AuthenticationEntryPoint() {
    // 没有登录
    @Override
    public void commence(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException, ServletException {
        // 根据请求是否为 AJAX 请求，以不同的方式响应未经身份验证的用户。对于 AJAX 请求，它发送JSON响应；对于非 AJAX 请求，它将用户重定向到登录页面
        String xRequestWith = httpServletRequest.getHeader("x-requested-with");
        if("XMLHttpRequest".equals(xRequestWith)){
            httpServletResponse.setContentType("application/plain;charset=utf-8");
            PrintWriter writer = httpServletResponse.getWriter();
            writer.write(CommunityUtil.getJSONString(403,"您还没有登录!"));
        }else{
            httpServletResponse.sendRedirect(httpServletRequest.getContextPath()+"/login");
        }
    }
})
.accessDeniedHandler(new AccessDeniedHandler() {
    // 权限不足
    @Override
    public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AccessDeniedException e) throws IOException, ServletException {
        // 根据请求是否为 AJAX 请求，以不同的方式响应用户没有访问权限的情况。对于 AJAX 请求，它发送JSON响应；对于非 AJAX 请求，它将用户重定向到错误页面
        String xRequestWith = httpServletRequest.getHeader("x-requested-with");
        if("XMLHttpRequest".equals(xRequestWith)){
            httpServletResponse.setContentType("application/plain;charset=utf-8");
            PrintWriter writer = httpServletResponse.getWriter();
            writer.write(CommunityUtil.getJSONString(403,"你没有访问此功能的权限!"));
        }else{
            httpServletResponse.sendRedirect(httpServletRequest.getContextPath()+"/denied");
        }
    }
});
```

实现退出功能,从redis中读出登录凭证，修改状态后再存入,Security底层默认会拦截/logout请求，进行退出处理，所以我们要覆盖它默认的逻辑，才能执行我们自己的退出代码

```java
http.logout().logoutUrl("/securitylogout");
```

**登录功能可能需要改进的地方：一个用户不退出，再用用这个用户登录会产生一个新的登录凭证，所以要先在插入登录凭证前先查询是否有该用户的凭证，有的话（询问其是否顶掉），把status置为1，再插入一个新的登录凭证。**



## 账号设置

```java
@RequestMapping(path = "/setting",method = RequestMethod.GET)
```

将头像保存在七牛云中，配置信息如下：

```java
# 七牛云
qiniu.key.access=密钥AK
qiniu.key.secret=密钥SK
qiniu.bucket.header.name=自己存放头像的云空间
qiniu.bucket.header.url=自己存放头像的云空间url
```

在get中生成上传文件名称和上传凭证返回给设置页面

在前端通过异步请求将图片发到到七牛云的云存储中

```javascript
$.ajax({
    url:"http://upload-cn-east-2.qiniup.com",
    method: "post",
    processData: false,
    contentType: false,
    data: new FormData($("#uploadForm")[0]),
    success: function (data) {
        //自己处理返回的数据，另外别忘记跟新头像访问路径
    }
});
```

更改数据库中的头像路径，并把redis中的该用户信息删除（也可以自己改成修改，但不是太必要，因为下次查询该用户时也会把该用户加到redis中）



## 发布帖子

```java
@RequestMapping(path = "/add",method = RequestMethod.POST)
@ResponseBody
```

通过post传来的帖子标题和内容，封装成discussPost对象中，并在过service对标题进行敏感词过滤和转义HTML标记(防止注入关键词导致页面变化)，然后插入到数据表中，封装event对象，再触发发帖事件通过kafka将该帖子异步发送到elasticsearch中

```java
Event event=new Event()
        .setTopic(TOPIC_PUBLISH)
        .setUserId(user.getId())
        .setEntityType(ENTITY_TYPE_COMMENT)
        .setEntityId(discussPost.getId());
eventProducer.fireEvent(event);
```

```java
//生产者
//处理事件
public void fireEvent(Event event){
    //将事件发送到指定的主题
    kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
}
```

```java
//消费者
//监听TOPIC_PUBLISH事件
@KafkaListener(topics = TOPIC_PUBLISH)
public void HandlePublish(ConsumerRecord record){
    if(record==null||record.value()==null){
        logger.error("消息内容为空！");
        return;
    }
    Event event = JSONObject.parseObject(record.value().toString(), Event.class);
    if (event==null) {
        logger.error("消息格式错误！");
        return;
    }

    //通过实体类id查询帖子
    DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
	//发送到ES中
    elasticsearchService.saveDiscussPost(post);
}
```

将刷新分数的帖子id加到redis中，在每次执行刷新帖子任务的时候刷新这些帖子的分数。

```java
//刷新帖子分数
@Override
public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    String redisKey = RedisKeyUtil.getPostScoreKey();
    BoundSetOperations operations=redisTemplate.boundSetOps(redisKey);

    if (operations.size()==0) {
        logger.info("[任务取消]，没有需要刷新的帖子!");
        return;
    }

    logger.info("[任务开始]，正在刷新帖子的分数："+operations.size());
    while (operations.size() > 0) {
        this.refresh((Integer)operations.pop());
    }

    logger.info("[任务结束]，帖子分数刷新完毕!");
}
```

刷新时要同时刷新数据库和ES中的帖子信息。

```java
private void refresh(int postId){
    DiscussPost post = discussPostService.findDiscussPostById(postId);
    if (post==null) {
        logger.error("该帖子不存在：id="+postId);
        return;
    }
    
    /*具体的的分数计算*/

    //更新帖子的分数
    discussPostService.updateScore(postId,score);

    //同步es中的数据
    post.setScore(score);
    elasticsearchService.saveDiscussPost(post);
}
```



## 显示帖子详情

包含：帖子的内容，帖子的回帖，回帖的回复，回复的回复，帖子的回帖数量，帖子的点赞功能，帖子的点赞数量，回帖的回复数量

```java
@RequestMapping(path = "/detail/{disPostId}",method = RequestMethod.GET)
```

从数据库的discuss_post中读到该帖子，然后，设置评论的分页信息，从数据库中的comment中读取limit条评论和每条评论对应的所有回复（entity=1,表示是评论；entity=2,表示是回复），再从Redis中读取它们点赞数量和登录用户的点赞状态，还要读取评论的回复数量、回复的作者及回复目标封装在一起返回给前端

```java
commentVoList
··commentVo
....comment				//评论
....user				//评论作者
....likeCount			//评论的点赞数量
....likeStatus			//评论的点赞状态
....replyCount			//评论的回复数量
....replyVoList
......replyVo
........reply			//回复
........user			//回复的作者
........target			//回复的目标（User）
........likeCount		//回复的点赞数量
........likeStatus		//回复的点赞状态
```



## 添加评论或回复

```java
@RequestMapping(path = "/add/{discussPostId}",method = RequestMethod.POST)
```

封装comment插入到数据库中，然后封装event，判断是评论还是回复，是评论的话从discuss_post中查询target，是回复的话从comment查询target,然后把target的id添加到event中，通过kafka让系统异步给目标发送系统消息(后面细讲系统怎么发送)，同时还要修改ES中的帖子的回帖数量。最后重定向到帖子详情页面。



## 显示系统消息列表

```java
@RequestMapping(path = "/notice/list",method = RequestMethod.GET)
```

查询数据库中的评论类通知、点赞类通知和关注类通知，根据message表中的conversation_id区分是评论类通知(comment)、还是点赞类通知(like)、还是关注类通知(follow)和用户之间的私信（{userId}_{userId}））,分别封装成messageVo，返回给前端.

```java
messageVo
..message			//消息
..user				//用户
..entityType		//实体类型
..entityId			//实体id
..postId			//帖子的id
..count				//消息总数量
..unreadCount		//未读的数量
```



## 显示系统消息详情

```java
@RequestMapping(path = "/notice/detail/{topic}",method = RequestMethod.GET)
```

设置分页信息，分页查询登录用户收到的系统发送的topic标题的消息，将消息和实体类等信息封装在一起发送给前端页面。

```java
noticeVoList
..noticeVo
....notice			//通知
....user			//用户
....entityType		//实体类类型
....entityId		//实体类id
....postId			//帖子id
....fromUser		//通知作者
```

最后要把读过的消息设为已读。



## 显示私信消息列表

```java
@RequestMapping(path = "/letter/list",method = RequestMethod.GET)
```

获取当前登录用户，设置分页信息，分页查询消息列表，封装成conversations,传给前端页面

```java
conversations
..map
....conversation		//最新的一条消息
....letterCount			//消息数量
....unreadCount			//未读消息的数量
....target				//目标用户
```

查询未读私信和未读系统消息的总数量，传给前端页面



## 显示私信详情

```java
@RequestMapping(path = "/letter/detail/{conversationId}",method = RequestMethod.GET)
```

设置分页信息，根据conversationId分页查询私信信息，封装成letter返回给前端页面

```java
letters
..map
....letter			//消息
....fromUser		//消息的发送者
target				//消息的目标
```

改变已读的消息为已读



## 发送私信

```java
@RequestMapping(path = "/letter/send",method = RequestMethod.POST)
@ResponseBody
```

通过目标用户的username获取target用户，拼接conversation_id，把数据封装成message，把message插入到数据库中。



## 点赞

```java
@RequestMapping(path = "/like",method = RequestMethod.POST)
@ResponseBody
```

将给帖子点了赞的用户id的集合，用户的总点赞量存入redis中

点赞：用户给帖子点赞和用户的总点赞量增加应该同进同退

取消点赞：用户给帖子取消点赞和用户的总点赞量减少也应该同进同退

所以我们要使用事务

```java
redisTemplate.execute(new SessionCallback() {
    @Override
    public Object execute(RedisOperations operations) throws DataAccessException {
        //谁给帖子点了赞的集合key
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, id);
        //用户的总点赞量的key
        String userLikeKey = RedisKeyUtil.getUserLikeKey(targetUserId);
        //判断用户是否点赞
        Boolean isMember = redisTemplate.opsForSet().isMember(entityLikeKey, userId);
        //开启事务
        operations.multi();
        if (isMember) {//已有点赞，就取消点赞
            redisTemplate.opsForSet().remove(entityLikeKey, userId);
            redisTemplate.opsForValue().decrement(userLikeKey);
        } else {//没有点赞，点赞成功
            redisTemplate.opsForSet().add(entityLikeKey, userId);
            redisTemplate.opsForValue().increment(userLikeKey);
        }
        return operations.exec();
    }
});
```



## 个人主页展示

```java
@RequestMapping(path = "/profile/{userId}",method = RequestMethod.GET)
```

通过userId查询用户，从redis中查询用户获得的点赞数量、用户关注的实体数量、关注用户的实体数量和用户是否关注该用户，传给前端页面。在前端页面中，要对关注按钮进行处理。

```html
<button type="button" 
	th:class="|btn ${hasFollowed?'btn-secondary':'btn-info'} btn-sm float-right mr-5 follow-btn|" 
	th:text="${hasFollowed?'已关注':'关注TA'}" 
	th:if="${loginUser!=null&&loginUser.id!=user.id}">
	 	关注TA
</button>
```



## 关注的用户列表

```java
@RequestMapping(path = "/followees/{userId}",method = RequestMethod.GET)
```

根据userId查询出用户，将用户传到前端页面。设置分页信息，分页查询该用户关注的用户，查询登录用户是否关注该用户关注的用户，把数据传到前端页面。关注按钮处理同个人主页展示一样。



## 粉丝列表

```
@RequestMapping(path = "/followers/{entityId}",method = RequestMethod.GET)
```

根据userId查询出用户，将用户传到前端页面。设置分页信息，分页查询该用户的粉丝，查询登录用户是否关注该用户的粉丝，把数据传到前端页面。关注按钮处理同个人主页展示一样。



## 统一处理异常

```java
@ControllerAdvice(annotations = Controller.class)
public class ExceptionAdvice {
    private static Logger logger= LoggerFactory.getLogger(ExceptionAdvice.class);

    @ExceptionHandler({Exception.class})
    public void handleException(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.error("服务器发生异常："+e.getMessage());
        for (StackTraceElement element : e.getStackTrace()) {
            logger.error(element.toString());
        }

        //用于判断请求是否为异步请求
        String xRequestedWith = request.getHeader("x-requested-with");
        if ("XMLHttpRequest".equals(xRequestedWith)) {
            response.setContentType("application/plain;charset=utf-8");
            PrintWriter writer = response.getWriter();
            writer.write(CommunityUtil.getJSONString(1,"服务器异常！"));
        }else{
            response.sendRedirect(request.getContextPath()+"/error");
        }
    }
}
```



## 通过AOP思想实现统一打印日志

```java
@Component
@Aspect
public class ServiceLogAspect {

    private static Logger logger= LoggerFactory.getLogger(ServiceLogAspect.class);

    @Pointcut("execution(* com.nowcoder.community.service.*.*(..))")
    public void pointcut(){

    }

    @Before("pointcut()")
    public void before(JoinPoint joinPoint){
        //用户[1.2.3.4]，在[XXX(时间)],访问了[com.nowcoder.community.service.xxx()].
        //获取用户ip
        ServletRequestAttributes attributes=(ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes==null){//常规调用，不是前端调用
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getRemoteHost();
        //获取时间
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        //获取访问的方法名
        String target=joinPoint.getSignature().getDeclaringTypeName()+"."+joinPoint.getSignature().getName();
        logger.info(String.format("用户[%s]，在[%s],访问了[%s].",ip,now,target));
    }
}
```



## 展示后台UV和DAU

通过DataInterceptor拦截器记录UV和DAU

```java
@RequestMapping(path = "/data",method = {RequestMethod.GET,RequestMethod.POST})
```

返回统计页面



```java
//统计网站UV
@RequestMapping(path = "/data/uv",method = RequestMethod.POST)
```

使用redis中的HyperLogLog数据类型保存UV，整理指定日期范围内的key，整合这些key的数据，返回统计结果

```java
redisTemplate.opsForHyperLogLog().size(redisKey);
```



```java
//统计网站DAU
@RequestMapping(path = "/data/dau",method = RequestMethod.POST)
```

在redis中保存DAU，整理指定日期范围内的key，整合这些key的数据，返回统计结果

**问题：前端页面使用的是form，两个form不能同时显示数据，可以改为异步提交**









































