package com.lc.malluser.service.impl;

import com.lc.malluser.common.constants.Constants;
import com.lc.malluser.common.exception.GlobalException;
import com.lc.malluser.common.keys.UserKey;
import com.lc.malluser.common.resp.ResponseEnum;
import com.lc.malluser.common.resp.ServerResponse;
import com.lc.malluser.common.utils.CookieUtil;
import com.lc.malluser.common.utils.MD5Util;
import com.lc.malluser.dao.UserMapper;
import com.lc.malluser.entity.User;
import com.lc.malluser.service.IUserService;

import com.lc.malluser.vo.UserResVO;
import lombok.extern.slf4j.Slf4j;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class UserServiceImpl implements IUserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private CuratorFramework zkClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public ServerResponse login(String username, String password) {
        //1.校验参数不能为空
        if(StringUtils.isEmpty(username) || StringUtils.isEmpty(password)){
            throw new GlobalException(ResponseEnum.ILLEGAL_ARGUMENTS);
        }
        //2.根据用户名去取用户信息（本系统用户名不能重复）
        User user = userMapper.selectUserByUsername(username);
        if(null==user){
            throw new GlobalException(ResponseEnum.USER_NOT_EXIST);
        }
        //3.密码校验
        String md5Passwd = MD5Util.MD5EncodeUtf8(password);
        if (!user.getPassword().equals(md5Passwd)){
            throw new GlobalException(ResponseEnum.PASSWORD_WRONG);
        }
        //4.登陆成功
        return ServerResponse.createBySuccess(user);
    }


    @Override
    public ServerResponse register(User user) {
        //1.校验参数是否为空
        if(StringUtils.isEmpty(user.getUsername()) ||
                StringUtils.isEmpty(user.getEmail()) ||
                StringUtils.isEmpty(user.getPassword()) ||
                StringUtils.isEmpty(user.getQuestion()) ||
                StringUtils.isEmpty(user.getAnswer())){
            throw new GlobalException(ResponseEnum.ILLEGAL_ARGUMENTS);
        }
        //---开启锁
        InterProcessLock lock = null;
        try {
            lock = new InterProcessMutex(zkClient, Constants.USER_REGISTER_DISTRIBUTE_LOCK_PATH);
            boolean retry = true;
            do {
                if (lock.acquire(3000, TimeUnit.MILLISECONDS)){
                    log.info(user.getEmail()+Thread.currentThread().getName()+"获取锁");
                    //2.参数没问题的话，就校验一下名字是否已经存在
                    ServerResponse response = this.checkValid(user.getUsername(),Constants.USERNAME);
                    if(!response.isSuccess()){
                        //说明用户名已经重复了
                        return response;
                    }
                    //3.再校验一下邮箱是否存在
                    response = this.checkValid(user.getEmail(),Constants.EMAIL);
                    if(!response.isSuccess()){
                        //说明用户名已经重复了
                        return response;
                    }
                    //4.重复校验通过之后就可以塞入这条数据了
                    user.setRole(Constants.Role.ROLE_CUSTOME);//普通用户
                    user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));
                    userMapper.insert(user);
                    //跳出循环
                    retry = false;
                }
                log.info("【获取锁失败，继续尝试...】");
                //可以适当休息一会
            }while (retry);
        }catch (Exception e){
            log.error("【校验用户所填的用户名或者密码出现问题】",e);
            throw new GlobalException(ResponseEnum.SERVER_ERROR);
        }finally {
            //---释放锁
            if(lock != null){
                try {
                    lock.release();
                    log.info(user.getEmail()+Thread.currentThread().getName()+"释放锁");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return ServerResponse.createBySuccess();
    }

    @Override
    public ServerResponse checkValid(String str, String type) {
        //校验参数是否为空
        if(StringUtils.isEmpty(str) || StringUtils.isEmpty(type)){
            throw new GlobalException(ResponseEnum.ILLEGAL_ARGUMENTS);
        }
        if(Constants.USERNAME.equalsIgnoreCase(type)){
            //如果是username类型，那么就根据str为username去数据库查询
            User user= userMapper.selectUserByUsername(str);
            if(null!=user){
                //说明数据库已经存在这个username的用户了，返回用户已存在
                return ServerResponse.createByError(ResponseEnum.USERNAME_EXISTED);
            }
        }else if(Constants.EMAIL.equals(type)){
            //如果是email类型，就根据str为email去数据库查询
            User user = userMapper.selectByEmail(str);
            if(null!=user){
                //说明数据库已经存在这个email的用户了，返回用户已存在
                return ServerResponse.createByError(ResponseEnum.EMAIL_EXISTED);
            }
        }
        return ServerResponse.createBySuccess();
    }

    @Override
    public ServerResponse getQuestionByUsername(String username) {
        //1.校验参数
        if(StringUtils.isEmpty(username)){
            return ServerResponse.createByError(ResponseEnum.ILLEGAL_ARGUMENTS);
        }
        //2.根据username去获取题目
        User user = userMapper.selectUserByUsername(username);
        if(user == null){
            return ServerResponse.createByError(ResponseEnum.USER_NOT_EXIST);
        }
        String question = user.getQuestion();
        if(StringUtils.isEmpty(question)){
            return ServerResponse.createByError(ResponseEnum.ILLEGAL_ARGUMENTS);
        }
        return ServerResponse.createBySuccess(question);
    }

    //回答对问题后返回一个UUID身份标志，并存入redis，当重置密码时需要验证
    @Override
    public ServerResponse checkAnswer(String username, String question, String answer) {
        //1.校验参数是否正确
        if(StringUtils.isEmpty(username) || StringUtils.isEmpty(question) || StringUtils.isEmpty(answer)){
            return ServerResponse.createByError(ResponseEnum.ILLEGAL_ARGUMENTS);
        }
        //此处可优化从redis中拿（当获取问题的时候放入redis中）
        //2.参数没有问题之后，就可以去校验答案是否正确了
        User user = userMapper.selectUserByUsername(username);
        if (null==user){
            return ServerResponse.createByError(ResponseEnum.ILLEGAL_ARGUMENTS);
        }
        if( user.getAnswer().equals(answer)){
////            //首先根据规则key去redis取，如果还有没有过期的key，就可以直接拿来用了，不用再重新生成
//            String forgetToken = commonCacheUtil.getCacheValue(Constants.TOKEN_PREFIX+username);
//            if(StringUtils.isEmpty(forgetToken)){
//                return ServerResponse.createBySuccess(forgetToken);
//            }
            //取不到值，并且答案是对的，那么就重新生成一下吧！
            String forgetToken = UUID.randomUUID().toString();
            stringRedisTemplate.opsForValue().set(new UserKey(forgetToken).getPrefix(), forgetToken, Constants.RedisCacheExtime.REDIS_FORGETTEN_PASSWORD_USER, TimeUnit.SECONDS);
            return ServerResponse.createBySuccess(forgetToken);
        }
        return ServerResponse.createByError(ResponseEnum.ANSWER_WRONG);
    }

    //
    @Override
    public ServerResponse forgetResetPasswd(String username, String passwordNew,String forgetToken) {
        //1.校验参数
        if(StringUtils.isEmpty(username) || StringUtils.isEmpty(passwordNew) ||StringUtils.isEmpty(forgetToken)){
            return ServerResponse.createByError(ResponseEnum.ILLEGAL_ARGUMENTS);
        }
        //2.根据username去获取用户
        User user = userMapper.selectUserByUsername(username);
        if(user == null){
            return ServerResponse.createByError(ResponseEnum.USER_NOT_EXIST);
        }
        //3.从redis中获取token，看是否超时
        String redisToken = stringRedisTemplate.opsForValue().get(new UserKey(forgetToken).getPrefix());
        if(redisToken.isEmpty()){
            return ServerResponse.createByErrorMessage("token已经过期，修改密码操作失败");
        }
        //4.看前端传过来的token与redis中取出来的token是否相等
        if(!redisToken.equals(forgetToken)){
            return ServerResponse.createByErrorMessage("token错误，修改密码操作失败");
        }
//        //5.判断密码是否重复
       String MD5Passwd = MD5Util.MD5EncodeUtf8(passwordNew);
//        if(user.getPassword().equals(MD5Passwd)){
//            return ServerResponse.createByErrorMessage("不要使用重复密码！");
//        }
        //6.重置密码
        user.setPassword(MD5Passwd);
        int result = userMapper.updateByPrimaryKeySelective(user);
        if(result > 0){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError(ResponseEnum.SERVER_ERROR);
    }

    @Override
    public ServerResponse resetPasswd(String passwordOld, String passwordNew, int userId) {
        //1.校验参数
        if(StringUtils.isEmpty(passwordOld) || StringUtils.isEmpty(passwordNew)){
            return ServerResponse.createByError(ResponseEnum.ILLEGAL_ARGUMENTS);
        }
        User user = userMapper.selectByPrimaryKey(userId);
        if (null==user){
            return ServerResponse.createByError(ResponseEnum.LOGIN_EXPIRED);
        }
        //2.校验老的密码
        String passwordOldMD5 = MD5Util.MD5EncodeUtf8(passwordOld);
        if(passwordOldMD5.equals(user.getPassword())){
            return ServerResponse.createByError(ResponseEnum.PASSWORD_WRONG);
        }
        //3.重置新的密码
        user.setPassword(MD5Util.MD5EncodeUtf8(passwordNew));
        int updateCount = userMapper.updateByPrimaryKeySelective(user);
        if(updateCount > 0){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByErrorMessage("更新密码失败");
    }

    @Override
    public ServerResponse updateInfomation(String email, String phone, String question, String answer, Integer userId) {
        //1.获取当前登陆用户
        User user = userMapper.selectByPrimaryKey(userId);
        if (user == null){
            return ServerResponse.createByError(ResponseEnum.LOGIN_EXPIRED);
        }

        //2.校验参数
        if(StringUtils.isEmpty(email) || StringUtils.isEmpty(phone) || StringUtils.isEmpty(question) || StringUtils.isEmpty(answer)){
            return ServerResponse.createByError(ResponseEnum.ILLEGAL_ARGUMENTS);
        }

        //2.修改用户信息应该并发不大，所以不用加锁了，这里校验邮箱是否重复
        Integer queryCount = userMapper.checkEmailValid(email,userId);
        if(queryCount > 0){
            //说明这个邮箱已经被其他用户占用了，所以不能使用
            return ServerResponse.createByErrorMessage("此邮箱已经被占用，换个试试~");
        }

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setEmail(email);
        updateUser.setPhone(phone);
        updateUser.setQuestion(question);
        updateUser.setAnswer(answer);

        int updateCount = userMapper.updateByPrimaryKeySelective(updateUser);

        if(updateCount > 0){
            return ServerResponse.createBySuccess();
        }

        return ServerResponse.createByErrorMessage("更新用户信息失败");
    }

    @Override
    public UserResVO getUserInfoFromDB(Integer userId) {
        UserResVO userResVO = new UserResVO();
        User userDB = userMapper.selectByPrimaryKey(userId);
        if(userDB != null){
            BeanUtils.copyProperties(userDB,userResVO );
        }
        return userResVO;
    }


}
