package com.example.oj.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.config.FileUploadConfig;
import com.example.oj.mapper.CourseResourceMapper;
import com.example.oj.model.entity.Course;
import com.example.oj.model.entity.CourseResource;
import com.example.oj.model.entity.User;
import com.example.oj.model.dto.course.CourseResourceUpdateRequest;
import com.example.oj.model.enums.CourseResourceTypeEnum;
import com.example.oj.model.vo.CourseResourceVO;
import com.example.oj.service.CourseResourceService;
import com.example.oj.service.CourseService;
import com.example.oj.utils.FileUploadUtils;
import com.example.oj.utils.FileUploadUtils.FileInfo;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 课程资源服务实现
 */
@Service
public class CourseResourceServiceImpl extends ServiceImpl<CourseResourceMapper, CourseResource>
        implements CourseResourceService {

    private final CourseService courseService;
    private final FileUploadConfig fileUploadConfig;

    public CourseResourceServiceImpl(CourseService courseService, FileUploadConfig fileUploadConfig) {
        this.courseService = courseService;
        this.fileUploadConfig = fileUploadConfig;
    }

    @Override
    public String uploadCover(long courseId, MultipartFile file, User loginUser) {
        // 权限校验（同时确保课程存在）
        courseService.checkCourseAuth(courseId, loginUser);
        String subDir = "course/cover/" + courseId;
        FileInfo info = FileUploadUtils.upload(file,
                fileUploadConfig.getBaseDirFile(),
                fileUploadConfig.getUrlPrefix(),
                subDir,
                fileUploadConfig.getCoverMaxSize(),
                fileUploadConfig.getCoverAllowedExt());
        // 更新课程封面地址
        Course update = new Course();
        update.setId(courseId);
        update.setCoverUrl(info.getFileUrl());
        courseService.updateById(update);
        return info.getFileUrl();
    }

    @Override
    public CourseResourceVO uploadResource(long courseId, String resourceType, String title,
                                           String description, Integer sortOrder,
                                           MultipartFile file, User loginUser) {
        // 权限校验
        courseService.checkCourseAuth(courseId, loginUser);
        CourseResourceTypeEnum typeEnum = CourseResourceTypeEnum.getEnumByValue(resourceType);
        if (typeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "非法的资源类型：" + resourceType);
        }

        // 按类型选择子目录、大小、后缀
        String subDir;
        long maxSize;
        Set<String> allowedExt;
        if (typeEnum == CourseResourceTypeEnum.VIDEO) {
            subDir = "course/video/" + courseId;
            maxSize = fileUploadConfig.getVideoMaxSize();
            allowedExt = fileUploadConfig.getVideoAllowedExt();
        } else {
            subDir = "course/ppt/" + courseId;
            maxSize = fileUploadConfig.getPptMaxSize();
            allowedExt = fileUploadConfig.getPptAllowedExt();
        }

        // 保存文件到本地
        FileInfo info = FileUploadUtils.upload(file,
                fileUploadConfig.getBaseDirFile(),
                fileUploadConfig.getUrlPrefix(),
                subDir,
                maxSize,
                allowedExt);

        // 写入资源记录
        CourseResource resource = new CourseResource();
        resource.setCourseId(courseId);
        resource.setResourceType(typeEnum.getValue());
        resource.setTitle(title);
        resource.setDescription(description);
        resource.setFileName(info.getOriginalFileName());
        resource.setFileUrl(info.getFileUrl());
        resource.setFilePath(info.getFilePath());
        resource.setFileSize(info.getFileSize());
        resource.setMimeType(info.getMimeType());
        resource.setSortOrder(sortOrder == null ? 0 : sortOrder);
        boolean saved = this.save(resource);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存资源记录失败");
        }
        return toResourceVO(resource);
    }

    @Override
    public boolean deleteResource(long id, User loginUser) {
        CourseResource resource = this.getById(id);
        if (resource == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "资源不存在");
        }
        courseService.checkCourseAuth(resource.getCourseId(), loginUser);
        // 第一版仅逻辑删除，不删物理文件
        return this.removeById(id);
    }

    @Override
    public boolean updateResource(CourseResourceUpdateRequest request, User loginUser) {
        CourseResource resource = this.getById(request.getId());
        if (resource == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "资源不存在");
        }
        courseService.checkCourseAuth(resource.getCourseId(), loginUser);

        CourseResource update = new CourseResource();
        update.setId(request.getId());
        update.setTitle(request.getTitle());
        update.setDescription(request.getDescription());
        if (request.getSortOrder() != null) {
            update.setSortOrder(request.getSortOrder());
        }
        return this.updateById(update);
    }

    @Override
    public List<CourseResourceVO> listResources(long courseId, String type) {
        LambdaQueryWrapper<CourseResource> wrapper = new LambdaQueryWrapper<CourseResource>()
                .eq(CourseResource::getCourseId, courseId)
                .eq(StrUtil.isNotBlank(type), CourseResource::getResourceType, type)
                .orderByAsc(CourseResource::getSortOrder);
        List<CourseResource> resources = this.list(wrapper);
        List<CourseResourceVO> voList = new ArrayList<>();
        for (CourseResource resource : resources) {
            voList.add(toResourceVO(resource));
        }
        return voList;
    }

    // ==================== 私有工具方法 ====================

    private CourseResourceVO toResourceVO(CourseResource resource) {
        CourseResourceVO vo = new CourseResourceVO();
        vo.setId(resource.getId());
        vo.setCourseId(resource.getCourseId());
        vo.setResourceType(resource.getResourceType());
        vo.setTitle(resource.getTitle());
        vo.setDescription(resource.getDescription());
        vo.setFileName(resource.getFileName());
        vo.setFileUrl(resource.getFileUrl());
        vo.setFileSize(resource.getFileSize());
        vo.setMimeType(resource.getMimeType());
        vo.setSortOrder(resource.getSortOrder());
        vo.setCreateTime(resource.getCreateTime());
        return vo;
    }
}
