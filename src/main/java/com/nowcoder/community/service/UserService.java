package com.nowcoder.community.service;

import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    public User findUserById(int id){
        return userMapper.selectById(id);
    }

//    public User selectByName(String username) {
//        return userMapper.selectByName(username);
//    }
//
//    public User selectByEmail(String email){
//        return userMapper.selectByEmail(email);
//    }
//
//    public int insertUser(User user) {
//        return userMapper.insertUser(user);
//    }
//
//    public int updateStatus(int id,int status){
//        return userMapper.updateStatus(id,status);
//    }
//
//    public int updateHeader(int id,String headerUrl){
//        return userMapper.updateHeader(id,headerUrl);
//    }
//
//    public int updatePassword(int id,String password){
//        return userMapper.updatePassword(id,password);
//    }
}
