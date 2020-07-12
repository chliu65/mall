package com.lc.malluser.dao;


import com.lc.malluser.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Mapper
//@Repository
public interface UserMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(User record);

    int insertSelective(User record);

    User selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(User record);

    int updateByPrimaryKey(User record);

//    User selectByUsername(String username);
//    @Select("select * from mmall_user where username=#{username}")
    User selectUserByUsername(@Param("username") String username);

    User selectByEmail(String str);//数据库能不能优化

    User getUserByUsername(@Param("username") String username);

    User getUserByUsernameQuestionAnswer(String username, String question, String answer);

    Integer checkEmailValid(@Param("email") String email, @Param("userId") Integer userId);
}
