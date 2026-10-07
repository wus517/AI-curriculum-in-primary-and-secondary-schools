package com.example.oj.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户视图对象（脱敏，不含密码）
 */
@Data
public class UserVO implements Serializable {

    private Long id;

    private String username;

    private String role;

    private Date createTime;
}
