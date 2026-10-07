package com.example.oj.service;

/**
 * 判题服务
 * <p>
 * 与代码沙箱（CodeSandbox）解耦，后续可平滑替换为异步 MQ + Docker 沙箱。
 */
public interface JudgeService {

    /**
     * 对指定提交记录执行判题，并回写判题结果
     *
     * @param submissionId 提交记录 id
     */
    void doJudge(long submissionId);
}
