package com.nowcoder.community.dao;

import com.nowcoder.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper {
    //通过entityType（区分评论和回复）,entityId分页查询
    List<Comment> selectCommentsByEntity(int entityType,int entityId,int offset,int limit);

    //通过entityType（区分评论和回复）,entityId查询数量
    int selectCountByEntity(int entityType,int entityId);

    //差入评论
    int insertComment(Comment comment);

    //根据id查询
    Comment selectCommentById(int id);
}
