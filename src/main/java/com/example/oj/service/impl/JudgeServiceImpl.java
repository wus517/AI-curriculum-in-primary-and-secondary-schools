package com.example.oj.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.oj.mapper.ProblemMapper;
import com.example.oj.mapper.SubmissionMapper;
import com.example.oj.mapper.TestCaseMapper;
import com.example.oj.model.entity.Problem;
import com.example.oj.model.entity.Submission;
import com.example.oj.model.entity.TestCase;
import com.example.oj.model.enums.JudgeStatusEnum;
import com.example.oj.sandbox.CodeSandbox;
import com.example.oj.sandbox.model.ExecuteCodeRequest;
import com.example.oj.sandbox.model.ExecuteCodeResponse;
import com.example.oj.service.JudgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 判题服务实现（同步版本）
 * <p>
 * 依赖 CodeSandbox 接口，与具体沙箱实现解耦。
 */
@Slf4j
@Service
public class JudgeServiceImpl implements JudgeService {

    private final SubmissionMapper submissionMapper;
    private final ProblemMapper problemMapper;
    private final TestCaseMapper testCaseMapper;
    private final CodeSandbox codeSandbox;

    public JudgeServiceImpl(SubmissionMapper submissionMapper,
                            ProblemMapper problemMapper,
                            TestCaseMapper testCaseMapper,
                            CodeSandbox codeSandbox) {
        this.submissionMapper = submissionMapper;
        this.problemMapper = problemMapper;
        this.testCaseMapper = testCaseMapper;
        this.codeSandbox = codeSandbox;
    }

    @Override
    public void doJudge(long submissionId) {
        // 1. 查询提交记录
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new RuntimeException("提交记录不存在: " + submissionId);
        }

        // 2. 标记为判题中
        submission.setStatus(JudgeStatusEnum.JUDGING.getValue());
        submissionMapper.updateById(submission);

        // 3. 查询题目与测试用例
        Problem problem = problemMapper.selectById(submission.getProblemId());
        if (problem == null) {
            updateSystemError(submission, "题目不存在");
            return;
        }

        List<TestCase> testCases = testCaseMapper.selectList(
                new QueryWrapper<TestCase>().eq("problem_id", problem.getId()));
        if (CollUtil.isEmpty(testCases)) {
            updateSystemError(submission, "题目没有测试用例");
            return;
        }

        // 4. 组装沙箱执行请求
        ExecuteCodeRequest request = new ExecuteCodeRequest();
        request.setCode(submission.getCode());
        request.setLanguage(submission.getLanguage());
        request.setInputList(testCases.stream().map(TestCase::getInput).collect(Collectors.toList()));
        request.setTimeLimit(problem.getTimeLimit());
        request.setMemoryLimit(problem.getMemoryLimit());

        // 5. 调用沙箱执行
        ExecuteCodeResponse response;
        try {
            response = codeSandbox.executeCode(request);
        } catch (Exception e) {
            log.error("沙箱执行异常, submissionId={}", submissionId, e);
            updateSystemError(submission, "沙箱执行异常: " + e.getMessage());
            return;
        }

        // 6. 根据沙箱状态判定最终结果
        String sandboxStatus = response.getStatus();
        String finalStatus;
        String errorMessage = null;

        if (JudgeStatusEnum.COMPILE_ERROR.getValue().equals(sandboxStatus)) {
            finalStatus = JudgeStatusEnum.COMPILE_ERROR.getValue();
            errorMessage = response.getMessage();
        } else if (JudgeStatusEnum.TIME_LIMIT_EXCEEDED.getValue().equals(sandboxStatus)) {
            finalStatus = JudgeStatusEnum.TIME_LIMIT_EXCEEDED.getValue();
        } else if (JudgeStatusEnum.RUNTIME_ERROR.getValue().equals(sandboxStatus)) {
            finalStatus = JudgeStatusEnum.RUNTIME_ERROR.getValue();
            errorMessage = response.getMessage();
        } else if (JudgeStatusEnum.SYSTEM_ERROR.getValue().equals(sandboxStatus)) {
            finalStatus = JudgeStatusEnum.SYSTEM_ERROR.getValue();
            errorMessage = response.getMessage();
        } else {
            // 运行成功（ACCEPTED），需要逐个比较输出
            finalStatus = compareOutputs(testCases, response.getOutputList());
            if (JudgeStatusEnum.WRONG_ANSWER.getValue().equals(finalStatus)) {
                errorMessage = "答案错误";
            }
        }

        // 7. 回写判题结果
        submission.setStatus(finalStatus);
        submission.setTimeUsed(response.getTimeUsed());
        submission.setMemoryUsed(response.getMemoryUsed());
        submission.setErrorMessage(errorMessage);
        submission.setJudgeInfo(buildJudgeInfo(response, testCases.size(), finalStatus));
        submissionMapper.updateById(submission);
    }

    /**
     * 比较用户输出与标准输出，忽略首尾及中间多余空白
     */
    private String compareOutputs(List<TestCase> testCases, List<String> outputList) {
        int total = testCases.size();
        for (int i = 0; i < total; i++) {
            String expected = testCases.get(i).getOutput();
            String actual = (outputList != null && i < outputList.size()) ? outputList.get(i) : null;
            if (!normalize(expected).equals(normalize(actual))) {
                return JudgeStatusEnum.WRONG_ANSWER.getValue();
            }
        }
        return JudgeStatusEnum.ACCEPTED.getValue();
    }

    /**
     * 规范化字符串：trim + 连续空白压缩为单个空格
     */
    private String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().replaceAll("\\s+", " ");
    }

    /**
     * 标记为系统错误
     */
    private void updateSystemError(Submission submission, String message) {
        submission.setStatus(JudgeStatusEnum.SYSTEM_ERROR.getValue());
        submission.setErrorMessage(message);
        submissionMapper.updateById(submission);
    }

    /**
     * 构造判题附加信息
     */
    private String buildJudgeInfo(ExecuteCodeResponse response, int total, String finalStatus) {
        Map<String, Object> info = new HashMap<>();
        info.put("message", finalStatus);
        info.put("timeUsed", response.getTimeUsed());
        info.put("memoryUsed", response.getMemoryUsed());
        info.put("total", total);
        return JSONUtil.toJsonStr(info);
    }
}
