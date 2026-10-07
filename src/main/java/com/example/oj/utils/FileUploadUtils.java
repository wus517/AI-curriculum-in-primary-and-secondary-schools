package com.example.oj.utils;

import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * 本地文件上传工具类
 * <p>
 * 职责：校验（空/大小/后缀）→ 生成 UUID 文件名 → 创建目录 → 防路径穿越 → 写入磁盘 → 返回文件信息
 */
public class FileUploadUtils {

    private FileUploadUtils() {
    }

    /**
     * 保存上传文件到本地
     *
     * @param file         上传文件
     * @param baseDir      本地存储根目录（绝对路径）
     * @param urlPrefix    访问 URL 前缀，例如 /uploads
     * @param subDir       子目录，例如 course/video/1
     * @param maxSize      最大字节数
     * @param allowedExts  允许的后缀集合（小写）
     * @return 文件信息
     */
    public static FileInfo upload(MultipartFile file, File baseDir, String urlPrefix,
                                  String subDir, long maxSize, Set<String> allowedExts) {
        // 1. 空校验
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传文件为空");
        }
        // 2. 大小校验
        long size = file.getSize();
        if (size > maxSize) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小超过限制：" + maxSize + " bytes");
        }
        // 3. 后缀校验
        String originalName = file.getOriginalFilename();
        String ext = getExtension(originalName);
        if (ext.isEmpty() || !allowedExts.contains(ext)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的文件类型：" + ext);
        }
        // 4. 规范化子目录（统一斜杠 + 禁止穿越）
        String safeSubDir = subDir == null ? "" : subDir.replace("\\", "/");
        if (safeSubDir.contains("..")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法的存储路径");
        }
        // 5. 生成 UUID 文件名，避免重名
        String newFileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        File targetDir = new File(baseDir, safeSubDir);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建目录失败：" + targetDir.getAbsolutePath());
        }
        File dest = new File(targetDir, newFileName);
        // 6. 防路径穿越：目标文件必须在 baseDir 之内
        assertWithinBaseDir(baseDir, dest);
        // 7. 写入磁盘
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件写入失败：" + e.getMessage());
        }
        // 8. 组装返回信息
        String prefix = (urlPrefix != null && urlPrefix.endsWith("/"))
                ? urlPrefix.substring(0, urlPrefix.length() - 1)
                : urlPrefix;
        String relativePath = safeSubDir.isEmpty() ? newFileName : safeSubDir + "/" + newFileName;

        FileInfo info = new FileInfo();
        info.setOriginalFileName(originalName);
        info.setStoredFileName(newFileName);
        info.setFilePath(dest.getAbsolutePath());
        info.setRelativePath(relativePath);
        info.setFileUrl(prefix + "/" + relativePath);
        info.setFileSize(size);
        info.setMimeType(file.getContentType());
        return info;
    }

    /**
     * 取文件后缀（小写，不含点）
     */
    private static String getExtension(String filename) {
        if (filename == null) {
            return "";
        }
        // 只取最后的文件名部分，防止 a/b/../c.jpg 之类
        String name = filename;
        int slash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        if (slash >= 0) {
            name = filename.substring(slash + 1);
        }
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1) {
            return "";
        }
        return name.substring(idx + 1).toLowerCase();
    }

    /**
     * 校验目标文件路径不会逃逸出 baseDir
     */
    private static void assertWithinBaseDir(File baseDir, File dest) {
        try {
            String baseCanonical = baseDir.getCanonicalPath();
            String destCanonical = dest.getCanonicalPath();
            if (!destCanonical.startsWith(baseCanonical + File.separator) && !destCanonical.equals(baseCanonical)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法的文件路径");
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "路径校验失败");
        }
    }

    /**
     * 上传结果信息
     */
    @Data
    public static class FileInfo {
        /** 原始文件名 */
        private String originalFileName;
        /** 存储后的文件名（UUID） */
        private String storedFileName;
        /** 服务器本地绝对路径 */
        private String filePath;
        /** 相对 baseDir 的路径 */
        private String relativePath;
        /** 访问 URL */
        private String fileUrl;
        /** 文件大小（byte） */
        private Long fileSize;
        /** MIME 类型 */
        private String mimeType;
    }
}
