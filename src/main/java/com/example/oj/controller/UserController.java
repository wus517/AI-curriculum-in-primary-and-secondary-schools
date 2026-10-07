package com.example.oj.controller;

import com.example.oj.common.BaseResponse;
import com.example.oj.common.ResultUtils;
import com.example.oj.model.entity.User;
import com.example.oj.model.dto.user.UserLoginRequest;
import com.example.oj.model.dto.user.UserRegisterRequest;
import com.example.oj.model.vo.UserVO;
import com.example.oj.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户接口")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@Valid @RequestBody UserRegisterRequest userRegisterRequest) {
        long id = userService.userRegister(userRegisterRequest);
        return ResultUtils.success(id);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public BaseResponse<UserVO> userLogin(@Valid @RequestBody UserLoginRequest userLoginRequest,
                                          HttpServletRequest request) {
        UserVO userVO = userService.userLogin(userLoginRequest, request);
        return ResultUtils.success(userVO);
    }

    /**
     * 获取当前登录用户
     */
    @GetMapping("/current")
    public BaseResponse<UserVO> getCurrentUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 登出
     */
    @GetMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }
}
