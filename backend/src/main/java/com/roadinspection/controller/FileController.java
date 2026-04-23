package com.roadinspection.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.roadinspection.common.Result;
import com.roadinspection.common.ResultCode;
import com.roadinspection.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;

@Tag(name = "文件管理")
@RestController
@RequestMapping("/upload")
public class FileController {

    @Value("${file.upload-path:./uploads}")
    private String uploadPath;
    
    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");
    private static final List<String> VIDEO_EXTENSIONS = Arrays.asList(".mp4", ".avi", ".mov", ".wmv", ".flv");

    @Operation(summary = "上传图片")
    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename).toLowerCase();
        
        if (!IMAGE_EXTENSIONS.contains(extension)) {
            throw new BusinessException("只能上传图片文件，支持格式: jpg, jpeg, png, gif, bmp, webp");
        }

        try {
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String fileName = IdUtil.fastSimpleUUID() + extension;
            String relativePath = datePath + "/" + fileName;
            String fullPath = uploadPath + "/" + relativePath;

            File destFile = new File(fullPath);
            FileUtil.mkParentDirs(destFile);
            file.transferTo(destFile);

            Map<String, String> result = new HashMap<>();
            result.put("fileName", fileName);
            result.put("filePath", fullPath);
            result.put("relativePath", relativePath);
            result.put("url", "/api/files/upload/" + relativePath);

            return Result.success(result);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR);
        }
    }

    @Operation(summary = "上传视频")
    @PostMapping("/video")
    public Result<Map<String, String>> uploadVideo(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename).toLowerCase();
        
        if (!VIDEO_EXTENSIONS.contains(extension)) {
            throw new BusinessException("只能上传视频文件，支持格式: mp4, avi, mov, wmv, flv");
        }

        try {
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String fileName = IdUtil.fastSimpleUUID() + extension;
            String relativePath = datePath + "/" + fileName;
            String fullPath = uploadPath + "/" + relativePath;

            File destFile = new File(fullPath);
            FileUtil.mkParentDirs(destFile);
            file.transferTo(destFile);

            Map<String, String> result = new HashMap<>();
            result.put("fileName", fileName);
            result.put("filePath", fullPath);
            result.put("relativePath", relativePath);
            result.put("url", "/api/files/upload/" + relativePath);

            return Result.success(result);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR);
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
