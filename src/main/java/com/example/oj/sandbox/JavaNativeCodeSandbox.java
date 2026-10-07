package com.example.oj.sandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.example.oj.model.enums.JudgeStatusEnum;
import com.example.oj.sandbox.model.ExecuteCodeRequest;
import com.example.oj.sandbox.model.ExecuteCodeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Java 本地代码沙箱
 * <p>
 * 执行流程：
 * 1. 创建临时工作目录 /tmp/oj-code/{uuid}
 * 2. 写入 Main.java
 * 3. javac 编译
 * 4. 对每个用例执行 java Main（标准输入喂入数据）
 * 5. 并发读取 stdout / stderr，超时强制销毁进程
 * 6. 返回每个用例输出
 * 7. finally 清理临时目录
 * <p>
 * 注意：第一版不做严格内存限制，仅按 memoryLimit 设置 -Xmx 软限制。
 */
@Slf4j
@Component
public class JavaNativeCodeSandbox implements CodeSandbox {

    /** 临时代码根目录 */
    private static final String CODE_DIR = "/tmp/oj-code";

    /** 用户代码固定类名 */
    private static final String CLASS_NAME = "Main";

    /** 编译超时（ms） */
    private static final long COMPILE_TIMEOUT = 30_000L;

    /** 默认单用例运行超时（ms） */
    private static final long DEFAULT_TIME_LIMIT = 5_000L;

    /** 默认堆内存上限（MB） */
    private static final long DEFAULT_MEMORY_LIMIT = 256L;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest request) {
        ExecuteCodeResponse response = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        response.setOutputList(outputList);
        response.setTimeUsed(0L);
        response.setMemoryUsed(0L);
        // 默认认为执行成功（答案是否正确交由判题服务比较）
        response.setStatus(JudgeStatusEnum.ACCEPTED.getValue());
        response.setMessage("ok");

        String code = request.getCode();
        List<String> inputList = request.getInputList();
        if (inputList == null || inputList.isEmpty()) {
            inputList = Collections.singletonList("");
        }
        long timeLimit = (request.getTimeLimit() == null || request.getTimeLimit() <= 0)
                ? DEFAULT_TIME_LIMIT : request.getTimeLimit();
        long memoryLimit = (request.getMemoryLimit() == null || request.getMemoryLimit() <= 0)
                ? DEFAULT_MEMORY_LIMIT : request.getMemoryLimit();

        // 1. 创建临时工作目录
        File workDir = new File(CODE_DIR, UUID.randomUUID().toString());
        if (!workDir.mkdirs()) {
            response.setStatus(JudgeStatusEnum.SYSTEM_ERROR.getValue());
            response.setMessage("无法创建工作目录");
            return response;
        }

        try {
            // 2. 写入 Main.java
            File javaFile = new File(workDir, CLASS_NAME + ".java");
            FileUtil.writeUtf8String(code, javaFile);

            // 3. 编译
            String compileMsg = compile(workDir);
            if (compileMsg != null) {
                response.setStatus(JudgeStatusEnum.COMPILE_ERROR.getValue());
                response.setMessage(compileMsg);
                pad(outputList, inputList.size());
                return response;
            }

            // 4. 逐个用例运行
            long maxTime = 0;
            for (String input : inputList) {
                RunResult result = run(workDir, input, timeLimit, memoryLimit);
                maxTime = Math.max(maxTime, result.timeUsed);
                response.setTimeUsed(maxTime);

                if (result.timeout) {
                    response.setStatus(JudgeStatusEnum.TIME_LIMIT_EXCEEDED.getValue());
                    response.setMessage("运行超时");
                    pad(outputList, inputList.size());
                    return response;
                }
                if (result.exitCode != 0) {
                    response.setStatus(JudgeStatusEnum.RUNTIME_ERROR.getValue());
                    outputList.add(result.stdout);
                    response.setMessage(StrUtil.isNotBlank(result.stderr) ? result.stderr : "运行错误");
                    pad(outputList, inputList.size());
                    return response;
                }
                outputList.add(result.stdout);
            }
            return response;

        } catch (Exception e) {
            log.error("沙箱执行异常", e);
            response.setStatus(JudgeStatusEnum.SYSTEM_ERROR.getValue());
            response.setMessage("系统错误：" + e.getMessage());
            pad(outputList, inputList.size());
            return response;
        } finally {
            // 5. 清理临时目录
            FileUtil.del(workDir);
        }
    }

    /**
     * 编译代码，返回 null 表示成功，否则返回错误信息
     */
    private String compile(File workDir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("javac", "-encoding", "UTF-8", "Main.java");
        pb.directory(workDir);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        StringBuilder out = new StringBuilder();
        Thread reader = drain(p.getInputStream(), out);

        boolean finished = p.waitFor(COMPILE_TIMEOUT, TimeUnit.MILLISECONDS);
        if (!finished) {
            p.destroyForcibly();
            reader.interrupt();
            return "编译超时";
        }
        reader.join(1000);
        if (p.exitValue() != 0) {
            String msg = out.toString();
            return StrUtil.isBlank(msg) ? "编译失败" : msg;
        }
        return null;
    }

    /**
     * 运行单个用例
     */
    private RunResult run(File workDir, String input, long timeLimit, long memoryLimit)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-Dfile.encoding=UTF-8",
                "-Xmx" + memoryLimit + "m",
                "-Xss256k",
                CLASS_NAME
        );
        pb.directory(workDir);
        // 不合并 stdout/stderr，需要分别获取用户输出与错误信息
        Process p = pb.start();

        // 喂入标准输入并关闭
        OutputStream os = p.getOutputStream();
        if (input != null) {
            os.write(input.getBytes(StandardCharsets.UTF_8));
        }
        os.close();

        // 并发读取 stdout / stderr，避免管道阻塞死锁
        StringBuilder stdoutSb = new StringBuilder();
        StringBuilder stderrSb = new StringBuilder();
        Thread outReader = drain(p.getInputStream(), stdoutSb);
        Thread errReader = drain(p.getErrorStream(), stderrSb);

        long start = System.currentTimeMillis();
        boolean finished = p.waitFor(timeLimit, TimeUnit.MILLISECONDS);
        long elapsed = System.currentTimeMillis() - start;

        RunResult result = new RunResult();
        result.timeUsed = elapsed;

        if (!finished) {
            // 超时：强制销毁进程
            p.destroyForcibly();
            outReader.interrupt();
            errReader.interrupt();
            result.timeout = true;
            return result;
        }

        outReader.join(1000);
        errReader.join(1000);

        result.timeout = false;
        result.exitCode = p.exitValue();
        result.stdout = stdoutSb.toString();
        result.stderr = stderrSb.toString();
        return result;
    }

    /**
     * 启动守护线程把输入流读入 StringBuilder
     */
    private Thread drain(InputStream is, StringBuilder sb) {
        Thread t = new Thread(() -> {
            try {
                sb.append(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException ignored) {
                // 进程被销毁时可能出现异常，忽略
            }
        });
        t.setDaemon(true);
        t.start();
        return t;
    }

    /**
     * 把输出列表补齐到指定长度（用 null 占位），保证与输入一一对应
     */
    private void pad(List<String> outputList, int total) {
        while (outputList.size() < total) {
            outputList.add(null);
        }
    }

    /**
     * 单次运行结果
     */
    private static class RunResult {
        boolean timeout;
        int exitCode = 0;
        String stdout = "";
        String stderr = "";
        long timeUsed = 0;
    }
}
