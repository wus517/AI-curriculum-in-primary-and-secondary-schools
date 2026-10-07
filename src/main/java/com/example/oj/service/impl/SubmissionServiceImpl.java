package com.example.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.mapper.SubmissionMapper;
import com.example.oj.model.entity.Problem;
import com.example.oj.model.entity.Submission;
import com.example.oj.model.dto.submission.SubmissionAddRequest;
import com.example.oj.model.enums.JudgeStatusEnum;
import com.example.oj.model.enums.LanguageEnum;
import com.example.oj.model.vo.SubmissionVO;
import com.example.oj.service.JudgeService;
import com.example.oj.service.ProblemService;
import com.example.oj.service.SubmissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 提交服务实现
 */
@Slf4j
@Service
public class SubmissionServiceImpl extends ServiceImpl<SubmissionMapper, Submission> implements SubmissionService {

    private final JudgeService judgeService;
    private final ProblemService problemService;

    public SubmissionServiceImpl(JudgeService judgeService, ProblemService problemService) {
        this.judgeService = judgeService;
        this.problemService = problemService;
    }

    @Override
    public long addSubmission(SubmissionAddRequest submissionAddRequest, Long userId) {
        // 校验题目存在
        Long problemId = submissionAddRequest.getProblemId();
        Problem problem = problemService.getById(problemId);
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        // 校验语言
        String language = submissionAddRequest.getLanguage();
        if (!LanguageEnum.isSupport(language)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "暂不支持该编程语言");
        }

        // 保存提交记录（PENDING）
        Submission submission = new Submission();
        submission.setUserId(userId);
        submission.setProblemId(problemId);
        submission.setLanguage(language);
        submission.setCode(submissionAddRequest.getCode());
        submission.setStatus(JudgeStatusEnum.PENDING.getValue());
        submission.setTimeUsed(0L);
        submission.setMemoryUsed(0L);
        boolean saved = this.save(submission);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "提交失败");
        }

        // 同步判题（后续可改为 MQ 异步）
        long submissionId = submission.getId();
        try {
            judgeService.doJudge(submissionId);
        } catch (Exception e) {
            log.error("判题异常, submissionId={}", submissionId, e);
            // 判题异常时标记为系统错误
            submission.setStatus(JudgeStatusEnum.SYSTEM_ERROR.getValue());
            submission.setErrorMessage("判题异常: " + e.getMessage());
            this.updateById(submission);
        }

        return submissionId;
    }

    @Override
    public SubmissionVO getSubmission(long id) {
        Submission submission = this.getById(id);
        if (submission == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return toSubmissionVO(submission);
    }

    @Override
    public IPage<SubmissionVO> listMySubmission(long userId, long pageNum, long pageSize) {
        Page<Submission> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Submission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.orderByDesc("id");
        IPage<Submission> submissionPage = this.page(page, queryWrapper);

        List<SubmissionVO> voList = new ArrayList<>();
        for (Submission submission : submissionPage.getRecords()) {
            voList.add(toSubmissionVO(submission));
        }

        Page<SubmissionVO> voPage = new Page<>(pageNum, pageSize, submissionPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 实体转 VO
     */
    private SubmissionVO toSubmissionVO(Submission submission) {
        SubmissionVO vo = new SubmissionVO();
        vo.setId(submission.getId());
        vo.setUserId(submission.getUserId());
        vo.setProblemId(submission.getProblemId());
        vo.setLanguage(submission.getLanguage());
        vo.setCode(submission.getCode());
        vo.setStatus(submission.getStatus());
        vo.setTimeUsed(submission.getTimeUsed());
        vo.setMemoryUsed(submission.getMemoryUsed());
        vo.setErrorMessage(submission.getErrorMessage());
        vo.setJudgeInfo(submission.getJudgeInfo());
        vo.setCreateTime(submission.getCreateTime());
        return vo;
    }
}
