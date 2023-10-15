package com.nowcoder.community.util;

public interface CommunityConstant {

    /*
    * 激活成功
    * */
    int ACTIVATION_SUCCESS=0;

    /*
    * 重复激活
    * */
    int ACTIVATION_REPEAT=1;

    /*
    * 激活失败
    * */
    int ACTIVATION_FAILURE=2;

    /*
    * 默认状态的登录凭证的超时时间
    * */
    int DEFAULT_EXPIRED_SECONDS=3600*12;

    /*
     * 记住状态的登录凭证的超时时间
     * */
    int REMEMBER_EXPIRED_SECONDS=3600*24*100;

    /*
    * 实体类型：帖子（评论）
    * */
    int ENTITY_TYPE_COMMENT=1;

    /*
     * 实体类型：评论（回复）
     * */
    int ENTITY_TYPE_REPLY=2;

    /*
     * 实体类型：用户
     * */
    int ENTITY_TYPE_USER=3;

    /*
     * 实体类型：给帖子点赞(为了更好的理解原项目而增加的变量)
     * */
    int LIKE_TYPE_POST=1;

    /*
     * 实体类型：给回帖和回复点赞(为了更好的理解原项目而增加的变量)
     * */
    int LIKE_TYPE_COMMENT=2;

    /*
    * 主题：评论
    * */
    String TOPIC_COMMENT="comment";

    /*
     * 主题：点赞
     * */
    String TOPIC_LIKE="like";

    /*
     * 主题：关注
     * */
    String TOPIC_FOLLOW="follow";

    /*
    * 系统用户ID
    * */
    int SYSTEM_USER_ID=1;
}
