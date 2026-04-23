package com.roadinspection.controller;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roadinspection.common.Result;
import com.roadinspection.entity.User;
import com.roadinspection.mapper.UserMapper;
import com.roadinspection.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final OperationLogService operationLogService;

    @Operation(summary = "分页查询用户列表")
    @GetMapping("/list")
    public Result<Page<User>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status) {
        
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        if (username != null && !username.isEmpty()) {
            wrapper.like(User::getUsername, username);
        }
        if (realName != null && !realName.isEmpty()) {
            wrapper.like(User::getRealName, realName);
        }
        if (role != null && !role.isEmpty()) {
            wrapper.like(User::getRoles, role);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.eq(User::getDeleted, 0);
        wrapper.orderByDesc(User::getCreateTime);
        
        return Result.success(userMapper.selectPage(pageParam, wrapper));
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.success(userMapper.selectById(id));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public Result<Void> save(@RequestBody Map<String, Object> userMap) {
        String username = (String) userMap.get("username");
        if (username == null || username.trim().isEmpty()) {
            return Result.error(400, "用户名不能为空");
        }
        username = username.trim();
        
        String password = (String) userMap.get("password");
        String realName = (String) userMap.get("realName");
        String email = (String) userMap.get("email");
        String phone = (String) userMap.get("phone");
        Integer status = userMap.get("status") != null ? Integer.valueOf(userMap.get("status").toString()) : 1;
        
        Object rolesObj = userMap.get("roles");
        String roles = "";
        if (rolesObj instanceof List) {
            List<?> rolesList = (List<?>) rolesObj;
            if (!rolesList.isEmpty()) {
                roles = rolesList.stream()
                    .map(Object::toString)
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
            }
        } else if (rolesObj instanceof String) {
            roles = (String) rolesObj;
        }
        
        User existing = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getDeleted, 0)
        );
        if (existing != null) {
            return Result.error(400, "用户名已存在");
        }
        
        if (password == null || password.isEmpty()) {
            password = "123456";
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setRealName(realName != null ? realName : "");
        user.setRoles(roles);
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(status);
        userMapper.insert(user);
        
        String operator = getCurrentUsername();
        operationLogService.logAdd(operator, "用户：" + username);
        
        return Result.success();
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> userMap) {
        User oldUser = userMapper.selectById(id);
        String realName = (String) userMap.get("realName");
        String email = (String) userMap.get("email");
        String phone = (String) userMap.get("phone");
        Integer status = userMap.get("status") != null ? Integer.valueOf(userMap.get("status").toString()) : 1;
        
        Object rolesObj = userMap.get("roles");
        String roles = "";
        if (rolesObj instanceof List) {
            List<?> rolesList = (List<?>) rolesObj;
            roles = rolesList.stream()
                .map(Object::toString)
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        } else if (rolesObj instanceof String) {
            roles = (String) rolesObj;
        }
        
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setRealName(realName);
            user.setRoles(roles);
            user.setEmail(email);
            user.setPhone(phone);
            user.setStatus(status);
            userMapper.updateById(user);
        }
        
        String operator = getCurrentUsername();
        operationLogService.logUpdate(operator, "用户：" + (oldUser != null ? oldUser.getUsername() : id));
        
        return Result.success();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        userMapper.deleteById(id);
        
        String operator = getCurrentUsername();
        operationLogService.logDelete(operator, "用户：" + (user != null ? user.getUsername() : id));
        
        return Result.success();
    }

    @Operation(summary = "更新用户状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        User user = userMapper.selectById(id);
        if (user != null) {
            Integer newStatus = body.get("status");
            user.setStatus(newStatus);
            userMapper.updateById(user);
            
            String operator = getCurrentUsername();
            if (newStatus == 1) {
                operationLogService.logEnable(operator, "用户：" + user.getUsername());
            } else {
                operationLogService.logDisable(operator, "用户：" + user.getUsername());
            }
        }
        return Result.success();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setPassword(BCrypt.hashpw("123456", BCrypt.gensalt()));
            userMapper.updateById(user);
            
            String operator = getCurrentUsername();
            operationLogService.logUpdate(operator, "用户密码：" + user.getUsername());
        }
        return Result.success();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/{id}/password")
    public Result<Void> changePassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            return Result.error(400, "当前密码错误");
        }
        
        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        userMapper.updateById(user);
        return Result.success();
    }

    @Operation(summary = "获取所有启用的用户")
    @GetMapping("/enabled")
    public Result<List<User>> getEnabledUsers() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, 1);
        wrapper.eq(User::getDeleted, 0);
        return Result.success(userMapper.selectList(wrapper));
    }
    
    private String getCurrentUsername() {
        String username = "系统";
        return username;
    }
}
