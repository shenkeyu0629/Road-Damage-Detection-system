package com.roadinspection.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.roadinspection.common.ResultCode;
import com.roadinspection.dto.LoginDTO;
import com.roadinspection.dto.LoginVO;
import com.roadinspection.entity.User;
import com.roadinspection.exception.BusinessException;
import com.roadinspection.mapper.UserMapper;
import com.roadinspection.security.JwtTokenProvider;
import com.roadinspection.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        User user = getByUsername(loginDTO.getUsername());
        
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }
        
        if (user.getStatus() != 1) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        
        user.setLastLoginTime(LocalDateTime.now());
        updateById(user);
        
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        
        LoginVO loginVO = new LoginVO();
        loginVO.setId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setRealName(user.getRealName());
        loginVO.setAvatar(user.getAvatar());
        loginVO.setToken(token);
        loginVO.setPermissions(new ArrayList<>());
        
        return loginVO;
    }

    @Override
    public User getByUsername(String username) {
        return lambdaQuery()
                .eq(User::getUsername, username)
                .eq(User::getDeleted, 0)
                .one();
    }
}
