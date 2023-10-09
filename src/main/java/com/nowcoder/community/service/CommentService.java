package com.nowcoder.community.service;

import com.nowcoder.community.dao.CommentMapper;
import com.nowcoder.community.entity.Comment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {
    @Autowired
    private CommentMapper commentMapper;

    public List<Comment> findCommentsByEntity(int entityType,int entityId,int offset,int limit){
        System.out.println(offset);
        System.out.println(limit);
        System.out.println(entityId);
        System.out.println(entityType);
        return commentMapper.selectCommentsByEntity(entityType, entityId, offset, limit);
    }

    public int findCountByEntity(int entityType,int entityId){
        return commentMapper.selectCountByEntity(entityType,entityId);
    }
}
