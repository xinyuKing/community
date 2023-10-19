package com.nowcoder.community.util;

import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;

public class RedisKeyUtil {
    private static final String SPLIT=":";
    private static final String PREFIX_ENTITY_LIKE="like:entity";
    private static final String PREFIX_USER_LIKE="like:user";
    private static final String PREFIX_FOLLOWEE="followee";
    private static final String PREFIX_FOLLOWER="follower";
    private static final String PREFIX_KAPTCHA="kaptcha";
    private static final String PREFIX_TICKET="ticket";
    private static final String PREFIX_USER="user";
    private static final String PREFIX_UV="uv";
    private static final String PREFIX_DAU="dau";

    //某个实体的赞
    // like:entity:entityType:id->set(userId)
    // id为Comment.id或DiscussPost.id
    public static String getEntityLikeKey(int entityType,int id){
        return PREFIX_ENTITY_LIKE+SPLIT+entityType+SPLIT+id;
    }

    //某个用户的赞
    // like:entity:userId->int
    public static String getUserLikeKey(int userId){
        return PREFIX_USER_LIKE+SPLIT+userId;
    }

    //某个用户关注的实体
    //followee:userId:entityType->zset(entityId,now)
    public static String getFolloweeKey(int userId,int entityType){
        return PREFIX_FOLLOWEE+SPLIT+userId+SPLIT+entityType;
    }

    //某个实体拥有的粉丝
    //followee:entityType:entityId->zset(userId,now)
    public static String getFollowerKey(int entityType,int entityId){
        return PREFIX_FOLLOWER+SPLIT+entityType+SPLIT+entityId;
    }

    //登录验证码
    public static String getKaptchaKey(String owner){
        return PREFIX_KAPTCHA+SPLIT+owner;
    }

    //登录的凭证
    public static String getTicketKey(String ticket){
        return PREFIX_TICKET+SPLIT+ticket;
    }

    //user
    public static String getUserKey(int userId){
        return PREFIX_USER+SPLIT+userId;
    }

    //单日uv:ip集合
    public static String getUVKey(String date){
        return PREFIX_UV+SPLIT+date;
    }

    //区间uv
    public static String getUVKey(String startDate,String endDate){
        return PREFIX_UV+SPLIT+startDate+SPLIT+endDate;
    }

    //单日活跃用户
    public static String getDAUKey(String date){
        return PREFIX_DAU+SPLIT+date;
    }

    //区间活跃用户
    public static String getDAUKey(String startDate,String endDate){
        return PREFIX_DAU+SPLIT+startDate+SPLIT+endDate;
    }
}
