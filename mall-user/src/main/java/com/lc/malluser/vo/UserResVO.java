package com.lc.malluser.vo;

import lombok.Data;

import java.util.Date;


@Data
public class UserResVO {
    private int id;
    private String username;
    private String email;
    private int role;
    private String phone;
    private String question;
    private String answer;
    private Date createTime;//返回前端的是时间戳
    private Date updateTime;
}
