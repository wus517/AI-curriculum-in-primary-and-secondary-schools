package com.example.oj.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.oj.model.entity.Submission;
import com.example.oj.model.dto.submission.SubmissionAddRequest;
import com.example.oj.model.vo.SubmissionVO;

/**
 * 提交服务
 */
public interface SubmissionService extends IService<Submission> {

    /**
     * 提交代码并同步判题
     *
     * @return 提交记录 id
     */
    long addSubmission(SubmissionAddRequest submissionAddRequest, Long userId);

    /**
     * 查询提交详情
     */
    SubmissionVO getSubmission(long id);

    /**
     * 分页查询当前用户的提交记录
     */
    IPage<SubmissionVO> listMySubmission(long userId, long pageNum, long pageSize);
}
