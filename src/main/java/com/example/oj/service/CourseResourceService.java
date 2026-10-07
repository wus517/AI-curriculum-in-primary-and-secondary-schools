package com.example.oj.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.oj.model.entity.CourseResource;
import com.example.oj.model.entity.User;
import com.example.oj.model.dto.course.CourseResourceUpdateRequest;
import com.example.oj.model.vo.CourseResourceVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 课程资源服务
 */
public interface CourseResourceService extends IService<CourseResource> {

    /**
     * 上传课程封面，更新 course.coverUrl，返回访问 URL
     */
    String uploadCover(long courseId, MultipartFile file, User loginUser);

    /**
     * 上传课程资源（视频或 PPT），返回资源 VO
     *
     * @param resourceType video / ppt
     */
    CourseResourceVO uploadResource(long courseId, String resourceType, String title,
                                    String description, Integer sortOrder,
                                    MultipartFile file, User loginUser);

    /**
     * 删除课程资源（逻辑删除，不删物理文件）
     */
    boolean deleteResource(long id, User loginUser);

    /**
     * 修改资源信息（标题/描述/排序，不涉及文件）
     */
    boolean updateResource(CourseResourceUpdateRequest request, User loginUser);

    /**
     * 查询课程资源列表
     *
     * @param type video / ppt，为空返回全部
     */
    List<CourseResourceVO> listResources(long courseId, String type);
}
