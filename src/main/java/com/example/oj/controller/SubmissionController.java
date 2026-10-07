package com.example.oj.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.oj.common.BaseResponse;
import com.example.oj.common.ResultUtils;
import com.example.oj.model.entity.User;
import com.example.oj.model.dto.submission.SubmissionAddRequest;
import com.example.oj.model.vo.SubmissionVO;
import com.example.oj.service.SubmissionService;
import com.example.oj.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 提交接口
 */
@RestController
@RequestMapping("/api/submission")
@Tag(name = "提交接口")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final UserService userService;

    public SubmissionController(SubmissionService submissionService, UserService userService) {
        this.submissionService = submissionService;
        this.userService = userService;
    }

    /**
     * 提交代码（同步判题）
     */
    @PostMapping("/add")
    public BaseResponse<Long> addSubmission(@Valid @RequestBody SubmissionAddRequest submissionAddRequest,
                                            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        long id = submissionService.addSubmission(submissionAddRequest, loginUser.getId());
        return ResultUtils.success(id);
    }

    /**
     * 查询提交详情
     */
    @GetMapping("/get")
    public BaseResponse<SubmissionVO> getSubmission(@RequestParam("id") long id,
                                                    HttpServletRequest request) {
        userService.getLoginUser(request);
        SubmissionVO submissionVO = submissionService.getSubmission(id);
        return ResultUtils.success(submissionVO);
    }

    /**
     * 分页查询当前用户的提交记录
     */
    @GetMapping("/list/my")
    public BaseResponse<IPage<SubmissionVO>> listMySubmission(@RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
                                                              @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
                                                              HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        IPage<SubmissionVO> page = submissionService.listMySubmission(loginUser.getId(), pageNum, pageSize);
        return ResultUtils.success(page);
    }
}
