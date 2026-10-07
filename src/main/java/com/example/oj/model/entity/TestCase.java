package com.example.oj.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 测试用例实体
 */
@Data
@TableName("test_case")
public class TestCase implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long problemId;

    private String input;

    private String output;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer isDelete;
}
