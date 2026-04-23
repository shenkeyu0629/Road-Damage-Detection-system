package com.roadinspection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.roadinspection.dto.LoginDTO;
import com.roadinspection.dto.LoginVO;
import com.roadinspection.entity.User;

public interface UserService extends IService<User> {
    LoginVO login(LoginDTO loginDTO);
    User getByUsername(String username);
}
