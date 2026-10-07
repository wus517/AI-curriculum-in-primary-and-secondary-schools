package com.example.oj.learning.tracking;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.mapper.SubmissionMapper;
import com.example.oj.model.entity.Submission;
import com.example.oj.model.enums.JudgeStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class JudgedResultService {
    private final SubmissionMapper submissions;
    private final TrackingService tracking;

    @Transactional(rollbackFor = Exception.class)
    public void save(Submission result) {
        if (JudgeStatusEnum.getEnumByValue(result.getStatus()) == null
                || Set.of("PENDING", "JUDGING").contains(result.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Terminal status required");
        }
        var saved = submissions.selectOne(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getId, result.getId()).last("FOR UPDATE"));
        if (saved == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        // A late exception or duplicate callback must not overwrite an already committed result.
        if (saved.getJudgedRevision() != null && saved.getJudgedRevision() > 0) return;
        saved.setStatus(result.getStatus()); saved.setTimeUsed(result.getTimeUsed() == null ? 0 : result.getTimeUsed());
        saved.setMemoryUsed(result.getMemoryUsed() == null ? 0 : result.getMemoryUsed());
        saved.setErrorMessage(result.getErrorMessage()); saved.setJudgeInfo(result.getJudgeInfo()); saved.setJudgedRevision(1L);
        if (submissions.update(null, new LambdaUpdateWrapper<Submission>().eq(Submission::getId, saved.getId())
                .set(Submission::getStatus, saved.getStatus()).set(Submission::getTimeUsed, saved.getTimeUsed())
                .set(Submission::getMemoryUsed, saved.getMemoryUsed()).set(Submission::getErrorMessage, saved.getErrorMessage())
                .set(Submission::getJudgeInfo, saved.getJudgeInfo()).set(Submission::getJudgedRevision, 1L)) != 1)
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        tracking.recordJudged(saved, 1);
    }
}
