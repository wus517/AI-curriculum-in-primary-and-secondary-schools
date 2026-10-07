package com.example.oj.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.oj.common.BaseResponse;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.common.ResultUtils;
import com.example.oj.model.entity.User;
import com.example.oj.model.dto.problem.ProblemAddRequest;
import com.example.oj.model.dto.problem.ProblemUpdateRequest;
import com.example.oj.model.vo.ProblemVO;
import com.example.oj.service.ProblemService;
import com.example.oj.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 题目接口
 */
@RestController
@RequestMapping("/api/problem")
@Tag(name = "题目接口")
public class ProblemController {

    private final ProblemService problemService;
    private final UserService userService;

    public ProblemController(ProblemService problemService, UserService userService) {
        this.problemService = problemService;
        this.userService = userService;
    }

    /**
     * 创建题目（仅管理员）
     */
    @PostMapping("/add")
    public BaseResponse<Long> addProblem(@Valid @RequestBody ProblemAddRequest problemAddRequest,
                                         HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        assertAdmin(loginUser);
        long id = problemService.addProblem(problemAddRequest, loginUser.getId());
        return ResultUtils.success(id);
    }

    /**
     * 修改题目（仅管理员）
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateProblem(@Valid @RequestBody ProblemUpdateRequest problemUpdateRequest,
                                               HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        assertAdmin(loginUser);
        boolean result = problemService.updateProblem(problemUpdateRequest);
        return ResultUtils.success(result);
    }

    /**
     * 删除题目（仅管理员）
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteProblem(@RequestParam("id") long id,
                                               HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        assertAdmin(loginUser);
        boolean result = problemService.deleteProblem(id);
        return ResultUtils.success(result);
    }

    /**
     * 查询题目详情（需登录）
     */
    @GetMapping("/get")
    public BaseResponse<ProblemVO> getProblem(@RequestParam("id") long id,
                                              HttpServletRequest request) {
        userService.getLoginUser(request);
        ProblemVO problemVO = problemService.getProblem(id);
        return ResultUtils.success(problemVO);
    }

    /**
     * 分页查询题目列表（需登录）
     */
    @GetMapping("/list")
    public BaseResponse<IPage<ProblemVO>> listProblem(@RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
                                                      @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
                                                      HttpServletRequest request) {
        userService.getLoginUser(request);
        IPage<ProblemVO> page = problemService.listProblem(pageNum, pageSize);
        return ResultUtils.success(page);
    }

    /**
     * 简单的管理员权限校验
     */
    private void assertAdmin(User user) {
        if (!"admin".equals(user.getRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }
}
