package com.example.oj.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.oj.model.entity.Problem;
import com.example.oj.model.dto.problem.ProblemAddRequest;
import com.example.oj.model.dto.problem.ProblemUpdateRequest;
import com.example.oj.model.vo.ProblemVO;

/**
 * 题目服务
 */
public interface ProblemService extends IService<Problem> {

    /**
     * 新增题目（含测试用例）
     *
     * @return 题目 id
     */
    long addProblem(ProblemAddRequest problemAddRequest, Long userId);

    /**
     * 修改题目（可携带测试用例覆盖）
     */
    boolean updateProblem(ProblemUpdateRequest problemUpdateRequest);

    /**
     * 逻辑删除题目（同步删除其测试用例）
     */
    boolean deleteProblem(long id);

    /**
     * 查询题目详情
     */
    ProblemVO getProblem(long id);

    /**
     * 分页查询题目列表（不含正文 content）
     */
    IPage<ProblemVO> listProblem(long pageNum, long pageSize);
}
