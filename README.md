## 首页

分页展示数据

分页按钮功能实现，

## 注册

get进入注册页面

实现邮件发送工具类

post提交注册数据

检查提交数据是否合理

发送激活邮件（成功跳转到中转页面，失败跳转到注册页面）

用户激活（激活成功通过中转页面跳转到登录页面，重复激活通过中转页面跳转到主页，**激活失败通过中转页面跳转到注册页面（再次注册时有问题，在注册时要判断该邮箱是否存在已激活的用户，存在未激活的用户，要先删除再重新注册）**）

## 登录

get进入登录页面

配置kaptcha参数信息

通过kaptcha生成验证码并存入session，生成图片传给浏览器

post提交登录数据

controller层获取session中的kaptcha，并验证

service层验证账号密码（错误时返回封装了Msg的map）后，生成并插入登录凭证（返回封装了ticket的map），controller再根据返回的map是否含有ticket跳转不同的页面（有的话把ticket装到cookie再重定向到主页，否则跳转登录页面）

主页的登录状态通过过滤器实现，先设置过滤器不过滤静态资源，在过滤器中的preHandle中先从cookie获取ticket（若不为null，通过ticket查询登录凭证，检查凭证是否有效，有效的话根据凭证查询到用户并装到单线程变量的user中（通过HostHolder实现）），在过滤器中的postHandle中获取单线程变量的user，若user不为空，在modelAndView中添加<"loginUser",user>,在过滤器中的afterCompletion清除单线程变量user。主页再通过是否存在loginUser来判断显示（消息功能和登录状态）或（登录功能和注册功能）

再实现退出功能

**登录功能可能需要改进的地方：先在插入登录凭证前先查询是否有该用户的凭证，有的话（询问其是否顶掉），把另一status置为1，再插入一个新的登录凭证。**

## 账号设置

get返回设置页面

post（enctype="multipart/form-data"，接收用MultipartFile headerImage）提交用户的新头像，修改用户头像（先把图片保存到服务器本地，再修改用户的头像地址（headerUrl）,最后重定向到主页）

实现头像（服务器本地存储，地址是服务器本地地址）的获取方式

**可能需要改进的地方：服务器中用户不使用的头像要及时处理（即更新头像时就清理，暂时不知道怎么实现）**

修改密码功能（**未实现**）

## 检查登录状态（未登录时不能访问一些页面，如setting）

自定义一个注解，该注解用于标识在拦截器中是否需要拦截带有该注解的方法

编写拦截器实现拦截未登录的用户访问带有该注解的controller层方法

## 过滤敏感词（为评论功能做准备）