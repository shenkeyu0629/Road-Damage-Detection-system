package com.roadinspection.dto;

import lombok.Data;

import java.util.List;

@Data
public class LoginVO {
    private Long id;
    private String username;
    private String realName;
    private String avatar;
    private String token;
    private List<String> permissions;
}
