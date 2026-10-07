package com.example.oj.sandbox;

import com.example.oj.sandbox.model.ExecuteCodeRequest;
import com.example.oj.sandbox.model.ExecuteCodeResponse;

/**
 * 代码沙箱接口
 * <p>
 * 与判题服务解耦：后续可新增 DockerCodeSandbox / RemoteCodeSandbox 等实现。
 */
public interface CodeSandbox {

    /**
     * 执行代码
     *
     * @param request 执行请求（代码、输入列表、限制）
     * @return 执行响应（输出列表、状态、耗时）
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest request);
}
