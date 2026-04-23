package com.roadinspection.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.roadinspection.common.Result;
import com.roadinspection.entity.User;
import com.roadinspection.mapper.UserMapper;
import com.roadinspection.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "角色管理")
@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
public class RoleController {

    private final UserMapper userMapper;
    private final OperationLogService operationLogService;

    @Operation(summary = "获取角色列表")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        List<Map<String, Object>> roles = new ArrayList<>();
        
        Map<String, Object> adminRole = new HashMap<>();
        adminRole.put("code", "admin");
        adminRole.put("name", "管理员");
        adminRole.put("description", "拥有系统最高管理权限，负责用户账户的创建与禁用、角色权限的分配以及操作日志的审计。");
        adminRole.put("permissions", Arrays.asList("road:manage", "section:manage", "monitor:view", "monitor:detect", "inspection:view", "inspection:manage", "record:view", "damage:history", "damage:disposal", "damage:analysis", "user:manage", "role:manage", "audit:manage", "log:manage"));
        adminRole.put("userCount", countUsersByRole("admin"));
        roles.add(adminRole);
        
        Map<String, Object> reviewerRole = new HashMap<>();
        reviewerRole.put("code", "reviewer");
        reviewerRole.put("name", "审核员");
        reviewerRole.put("description", "承担病害标注的审核工作，对自动识别结果进行确认、修正或驳回，并可查看病害统计数据和处置记录。");
        reviewerRole.put("permissions", Arrays.asList("monitor:view", "inspection:view", "record:view", "damage:history", "damage:disposal", "damage:analysis", "audit:manage"));
        reviewerRole.put("userCount", countUsersByRole("reviewer"));
        roles.add(reviewerRole);
        
        Map<String, Object> inspectorRole = new HashMap<>();
        inspectorRole.put("code", "inspector");
        inspectorRole.put("name", "巡检员");
        inspectorRole.put("description", "主要负责道路图像的采集与上传，可以查看巡检任务和记录，以及病害统计信息。");
        inspectorRole.put("permissions", Arrays.asList("monitor:view", "inspection:view", "record:view", "damage:history"));
        inspectorRole.put("userCount", countUsersByRole("inspector"));
        roles.add(inspectorRole);
        
        return Result.success(roles);
    }
    
    private long countUsersByRole(String roleCode) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0);
        wrapper.eq(User::getRoles, roleCode);
        return userMapper.selectCount(wrapper);
    }

    @Operation(summary = "获取角色详情")
    @GetMapping("/{code}")
    public Result<Map<String, Object>> getByCode(@PathVariable String code) {
        Map<String, Object> role = new HashMap<>();
        
        switch (code) {
            case "admin":
                role.put("code", "admin");
                role.put("name", "管理员");
                role.put("description", "拥有系统最高管理权限，负责用户账户的创建与禁用、角色权限的分配以及操作日志的审计。");
                role.put("permissions", Arrays.asList("road:manage", "section:manage", "monitor:view", "monitor:detect", "inspection:view", "inspection:manage", "record:view", "damage:history", "damage:disposal", "damage:analysis", "user:manage", "role:manage", "audit:manage", "log:manage"));
                break;
            case "reviewer":
                role.put("code", "reviewer");
                role.put("name", "审核员");
                role.put("description", "承担病害标注的审核工作，对自动识别结果进行确认、修正或驳回，并可查看病害统计数据和处置记录。");
                role.put("permissions", Arrays.asList("monitor:view", "inspection:view", "record:view", "damage:history", "damage:disposal", "damage:analysis", "audit:manage"));
                break;
            case "inspector":
                role.put("code", "inspector");
                role.put("name", "巡检员");
                role.put("description", "主要负责道路图像的采集与上传，可以查看由系统自动检测的病害结果，但无权对检测数据进行修改或审核。");
                role.put("permissions", Arrays.asList("detect:upload", "detect:view", "statistics:view"));
                break;
            default:
                return Result.error(404, "角色不存在");
        }
        
        return Result.success(role);
    }

    @Operation(summary = "更新角色")
    @PutMapping("/{code}")
    public Result<Void> update(@PathVariable String code, @RequestBody Map<String, Object> data) {
        String operator = getCurrentUsername();
        String roleName = getRoleName(code);
        operationLogService.logUpdate(operator, "角色权限：" + roleName);
        return Result.success();
    }
    
    private String getCurrentUsername() {
        return "系统";
    }
    
    private String getRoleName(String code) {
        switch (code) {
            case "admin": return "管理员";
            case "reviewer": return "审核员";
            case "inspector": return "巡检员";
            default: return code;
        }
    }
}
