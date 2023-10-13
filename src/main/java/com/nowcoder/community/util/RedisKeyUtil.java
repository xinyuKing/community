package com.nowcoder.community.util;

public class RedisKeyUtil {
    private static final String SPLIT=":";
    private static final String PREFIX_ENTITY_LIKE="like:entity";
    private static final String PREFIX_USER_LIKE="like:user";

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
}
