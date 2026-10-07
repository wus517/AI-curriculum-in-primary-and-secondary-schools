
package com.example.oj.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.mapper.ProblemMapper;
import com.example.oj.mapper.TestCaseMapper;
import com.example.oj.model.entity.Problem;
import com.example.oj.model.entity.TestCase;
import com.example.oj.model.dto.problem.ProblemAddRequest;
import com.example.oj.model.dto.problem.ProblemUpdateRequest;
import com.example.oj.model.vo.ProblemVO;
import com.example.oj.service.ProblemService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 题目服务实现
 */
@Service
public class ProblemServiceImpl extends ServiceImpl<ProblemMapper, Problem> implements ProblemService {

    private final TestCaseMapper testCaseMapper;

    public ProblemServiceImpl(TestCaseMapper testCaseMapper) {
        this.testCaseMapper = testCaseMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addProblem(ProblemAddRequest problemAddRequest, Long userId) {
        Problem problem = new Problem();
        BeanUtils.copyProperties(problemAddRequest, problem);
        // tags 由 List 转成 JSON 字符串存储
        problem.setTags(toTagsJson(problemAddRequest.getTags()));
        problem.setCreateUserId(userId);
        problem.setDifficulty(problemAddRequest.getDifficulty() == null ? 1 : problemAddRequest.getDifficulty());

        boolean saved = this.save(problem);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建题目失败");
        }

        // 保存测试用例
        saveTestCases(problem.getId(), problemAddRequest.getTestCases());

        return problem.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateProblem(ProblemUpdateRequest problemUpdateRequest) {
        Long id = problemUpdateRequest.getId();
        Problem oldProblem = this.getById(id);
        if (oldProblem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        Problem problem = new Problem();
        BeanUtils.copyProperties(problemUpdateRequest, problem);
        problem.setId(id);
        // tags 处理
        if (problemUpdateRequest.getTags() != null) {
            problem.setTags(toTagsJson(problemUpdateRequest.getTags()));
        }

        boolean updated = this.updateById(problem);

        // 若传入了测试用例，则覆盖原有用例
        if (problemUpdateRequest.getTestCases() != null) {
            // 逻辑删除旧用例
            TestCase toDelete = new TestCase();
            toDelete.setProblemId(id);
            testCaseMapper.delete(new QueryWrapper<>(toDelete));
            saveTestCases(id, problemUpdateRequest.getTestCases());
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteProblem(long id) {
        Problem problem = this.getById(id);
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 逻辑删除题目
        boolean removed = this.removeById(id);
        // 逻辑删除其测试用例
        TestCase toDelete = new TestCase();
        toDelete.setProblemId(id);
        testCaseMapper.delete(new QueryWrapper<>(toDelete));
        return removed;
    }

    @Override
    public ProblemVO getProblem(long id) {
        Problem problem = this.getById(id);
        if (problem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return toProblemVO(problem, true);
    }

    @Override
    public IPage<ProblemVO> listProblem(long pageNum, long pageSize) {
        Page<Problem> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Problem> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("id");
        IPage<Problem> problemPage = this.page(page, queryWrapper);

        // 转换为 VO（列表不返回正文 content）
        List<ProblemVO> voList = new ArrayList<>();
        for (Problem problem : problemPage.getRecords()) {
            voList.add(toProblemVO(problem, false));
        }

        Page<ProblemVO> voPage = new Page<>(pageNum, pageSize, problemPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    // ----------------------- 私有工具方法 -----------------------

    /**
     * 保存题目测试用例
     */
    private void saveTestCases(Long problemId, List<TestCase> testCases) {
        if (CollUtil.isEmpty(testCases)) {
            return;
        }
        for (TestCase testCase : testCases) {
            if (StrUtil.hasBlank(testCase.getInput(), testCase.getOutput())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "测试用例的输入输出不能为空");
            }
            TestCase tc = new TestCase();
            tc.setProblemId(problemId);
            tc.setInput(testCase.getInput());
            tc.setOutput(testCase.getOutput());
            testCaseMapper.insert(tc);
        }
    }

    /**
     * 标签列表转 JSON 字符串
     */
    private String toTagsJson(List<String> tags) {
        if (CollUtil.isEmpty(tags)) {
            return "[]";
        }
        return JSONUtil.toJsonStr(tags);
    }

    /**
     * 实体转 VO，includeContent 控制是否返回正文
     */
    private ProblemVO toProblemVO(Problem problem, boolean includeContent) {
        ProblemVO vo = new ProblemVO();
        vo.setId(problem.getId());
        vo.setTitle(problem.getTitle());
        vo.setContent(includeContent ? problem.getContent() : null);
        vo.setInputDesc(problem.getInputDesc());
        vo.setOutputDesc(problem.getOutputDesc());
        vo.setSampleInput(problem.getSampleInput());
        vo.setSampleOutput(problem.getSampleOutput());
        vo.setTimeLimit(problem.getTimeLimit());
        vo.setMemoryLimit(problem.getMemoryLimit());
        vo.setDifficulty(problem.getDifficulty());
        vo.setCreateUserId(problem.getCreateUserId());
        vo.setCreateTime(problem.getCreateTime());
        // tags JSON 字符串 -> List
        if (StrUtil.isNotBlank(problem.getTags())) {
            vo.setTags(JSONUtil.toList(problem.getTags(), String.class));
        }
        return vo;
    }
}
