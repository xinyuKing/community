package com.nowcoder.community.service;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class LikeService implements CommunityConstant {
    @Autowired
    private RedisTemplate redisTemplate;

    //点赞取消点赞功能
    public void like(int userId,int entityType,int id,int targetUserId){
//        String entityLikeKey= RedisKeyUtil.getEntityLikeKey(entityType,id);
//        //判断用户是否点赞
//        Boolean isMember = redisTemplate.opsForSet().isMember(entityLikeKey, userId);
//        if(isMember){//已有点赞，就取消点赞
//            redisTemplate.opsForSet().remove(entityLikeKey,userId);
//        }else {//没有点赞，点赞成功
//            redisTemplate.opsForSet().add(entityLikeKey,userId);
//        }
        //优化重构
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                //谁给帖子点了赞的集合key
                String entityLikeKey= RedisKeyUtil.getEntityLikeKey(entityType,id);
                //用户的总点赞量的key
                String userLikeKey=RedisKeyUtil.getUserLikeKey(targetUserId);
                //判断用户是否点赞
                Boolean isMember = redisTemplate.opsForSet().isMember(entityLikeKey, userId);
                //开启事务
                operations.multi();
                if(isMember){//已有点赞，就取消点赞
                    redisTemplate.opsForSet().remove(entityLikeKey,userId);
                    redisTemplate.opsForValue().decrement(userLikeKey);
                }else {//没有点赞，点赞成功
                    redisTemplate.opsForSet().add(entityLikeKey,userId);
                    redisTemplate.opsForValue().increment(userLikeKey);
                }
                return operations.exec();
            }
        });
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

    //查询每个用户获得的数量
    public int findUserLikeCount(int userId){
        String userLikeKey=RedisKeyUtil.getUserLikeKey(userId);
        Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey);

        return count==null?0:count.intValue();
    }
}
