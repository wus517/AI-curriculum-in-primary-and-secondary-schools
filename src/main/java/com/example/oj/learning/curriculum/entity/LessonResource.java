package com.example.oj.learning.curriculum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("lesson_resource")
public class LessonResource {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long lessonId;
    private Long resourceId;
    private Integer sortOrder;
    private Integer active;
}
