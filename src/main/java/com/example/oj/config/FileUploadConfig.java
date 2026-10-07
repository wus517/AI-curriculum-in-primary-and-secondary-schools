package com.example.oj.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件上传配置：
 * 1. 读取 application.yml 中的存储路径、大小、后缀限制
 * 2. 将本地 uploads 目录映射到 /uploads/** 静态资源访问
 */
@Slf4j
@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    @Value("${oj.upload.base-dir:uploads}")
    private String baseDir;

    @Value("${oj.upload.url-prefix:/uploads}")
    private String urlPrefix;

    @Value("${oj.upload.cover.max-size:5242880}")
    private long coverMaxSize;

    @Value("${oj.upload.video.max-size:524288000}")
    private long videoMaxSize;

    @Value("${oj.upload.ppt.max-size:104857600}")
    private long pptMaxSize;

    @Value("${oj.upload.cover.allowed-ext:jpg,jpeg,png,webp}")
    private String coverAllowedExt;

    @Value("${oj.upload.video.allowed-ext:mp4,mov,avi,mkv}")
    private String videoAllowedExt;

    @Value("${oj.upload.ppt.allowed-ext:ppt,pptx,pdf}")
    private String pptAllowedExt;

    @PostConstruct
    public void init() {
        File dir = getBaseDirFile();
        if (!dir.exists() && dir.mkdirs()) {
            log.info("创建上传根目录: {}", dir.getAbsolutePath());
        }
        log.info("文件上传配置：baseDir={}, urlPrefix={}", dir.getAbsolutePath(), urlPrefix);
    }

    /**
     * 静态资源映射：/uploads/** -> 本地 uploads 目录
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File dir = getBaseDirFile();
        // 必须以 / 结尾且为 file: 协议
        String location = dir.toURI().toString();
        registry.addResourceHandler(urlPrefix + "/**").addResourceLocations(location);
    }

    /**
     * 解析为绝对路径的根目录
     */
    public File getBaseDirFile() {
        File dir = new File(baseDir);
        return dir.getAbsoluteFile();
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    public long getCoverMaxSize() {
        return coverMaxSize;
    }

    public long getVideoMaxSize() {
        return videoMaxSize;
    }

    public long getPptMaxSize() {
        return pptMaxSize;
    }

    public Set<String> getCoverAllowedExt() {
        return split(coverAllowedExt);
    }

    public Set<String> getVideoAllowedExt() {
        return split(videoAllowedExt);
    }

    public Set<String> getPptAllowedExt() {
        return split(pptAllowedExt);
    }

    /**
     * 逗号分隔字符串转小写后缀集合
     */
    private Set<String> split(String ext) {
        if (ext == null || ext.isBlank()) {
            return new HashSet<>();
        }
        return Arrays.stream(ext.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }
}
