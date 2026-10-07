package com.example.oj.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.oj.model.entity.User;
import com.example.oj.model.vo.UserVO;
import com.example.oj.model.dto.user.UserLoginRequest;
import com.example.oj.model.dto.user.UserRegisterRequest;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户服务
 */
public interface UserService extends IService<User> {

    /** session 中保存登录用户的 key */
    String USER_LOGIN_STATE = "loginUser";

    /**
     * 用户注册
     *
     * @return 新用户 id
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录，并将登录态写入 session
     *
     * @return 脱敏后的用户信息
     */
    UserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

    /**
     * 获取当前登录用户，未登录抛出业务异常
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 登出
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 实体转视图对象（脱敏）
     */
    UserVO getUserVO(User user);
}
