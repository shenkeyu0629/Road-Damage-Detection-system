package com.roadinspection.service;

import com.roadinspection.entity.OperationLog;
import com.roadinspection.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public void log(String username, String operationType, String operationDesc) {
        OperationLog log = new OperationLog();
        log.setUsername(username);
        log.setOperationType(operationType);
        log.setOperationDesc(operationDesc);
        log.setOperationTime(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    public void logAdd(String username, String target) {
        log(username, "新增", "新增" + target);
    }

    public void logUpdate(String username, String target) {
        log(username, "修改", "修改" + target);
    }

    public void logDelete(String username, String target) {
        log(username, "删除", "删除" + target);
    }

    public void logEnable(String username, String target) {
        log(username, "启用", "启用" + target);
    }

    public void logDisable(String username, String target) {
        log(username, "禁用", "禁用" + target);
    }

    public void logAuditPass(String username, String target) {
        log(username, "审核通过", "审核通过：" + target);
    }

    public void logAuditReject(String username, String target) {
        log(username, "审核不通过", "审核不通过：" + target);
    }
}
