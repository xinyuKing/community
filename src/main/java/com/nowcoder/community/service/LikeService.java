package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LikeService {
    @Autowired
    private RedisTemplate redisTemplate;

    //点赞取消点赞功能
    public void like(int userId,int entityType,int id){
        String entityLikeKey= RedisKeyUtil.getEntityLikeKey(entityType,id);
        //判断用户是否点赞
        Boolean isMember = redisTemplate.opsForSet().isMember(entityLikeKey, userId);
        if(isMember){//已有点赞，就取消点赞
            redisTemplate.opsForSet().remove(entityLikeKey,userId);
        }else {//没有点赞，点赞成功
            redisTemplate.opsForSet().add(entityLikeKey,userId);
        }
    }

    //查询实体点赞的数量
    public long findEntityLikeCount(int entityType,int id){
        String entityLikeKey= RedisKeyUtil.getEntityLikeKey(entityType,id);
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    //查询某人对某实体的点赞状态
    public int findEntityLikeStatus(int userId,int entityType,int id){
        String entityLikeKey= RedisKeyUtil.getEntityLikeKey(entityType,id);
        return redisTemplate.opsForSet().isMember(entityLikeKey,userId)?1:0;
    }
}
