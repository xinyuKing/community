package com.nowcoder.community.dao;

import com.nowcoder.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {
    List<DiscussPost> selectDiscussPosts(int userId,int offset,int limit);

    //用于给参数起别名
    //如果方法只有一个参数，并且在<if>中使用，就必须加别名
    int selectDiscussPostRows(@Param("userId") int userId);

    //插入帖子
    int insertDiscussPostRows(DiscussPost discussPost);

    //通过id查询帖子
    DiscussPost selectDiscussPostById(int id);

    //更新帖子的数量
    int updateCommentCount(int id,int commentCount);

    // 更新帖子的置顶状态
    int updateType(int id,int type);

    // 更新帖子的精华状态
    int updateStatus(int id,int status);
}
