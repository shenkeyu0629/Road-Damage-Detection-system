package com.roadinspection.controller;

import com.roadinspection.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Tag(name = "文件存储管理")
@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
public class FileStorageController {

    @Value("${app.storage.base-path:./storage}")
    private String basePath;

    @Operation(summary = "初始化存储结构")
    @PostMapping("/init")
    public Result<Map<String, Object>> initStorage() {
        try {
            Path storagePath = Paths.get(basePath);
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("basePath", storagePath.toAbsolutePath().toString());
            result.put("createTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "初始化存储结构失败: " + e.getMessage());
        }
    }

    @Operation(summary = "创建道路文件夹")
    @PostMapping("/road/{roadId}")
    public Result<Map<String, Object>> createRoadFolder(
            @PathVariable Long roadId,
            @RequestParam String roadName,
            @RequestParam(required = false) String roadCode) {
        try {
            String folderName = sanitizeFolderName(roadName);
            if (roadCode != null && !roadCode.isEmpty()) {
                folderName = roadCode + "_" + folderName;
            }
            
            Path roadPath = Paths.get(basePath, folderName);
            if (!Files.exists(roadPath)) {
                Files.createDirectories(roadPath);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("path", roadPath.toAbsolutePath().toString());
            result.put("folderName", folderName);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "创建道路文件夹失败: " + e.getMessage());
        }
    }

    @Operation(summary = "创建路段文件夹")
    @PostMapping("/section/{sectionId}")
    public Result<Map<String, Object>> createSectionFolder(
            @PathVariable Long sectionId,
            @RequestParam Long roadId,
            @RequestParam String roadName,
            @RequestParam String sectionName,
            @RequestParam(required = false) String roadCode,
            @RequestParam(required = false) String sectionCode) {
        try {
            String roadFolderName = sanitizeFolderName(roadName);
            if (roadCode != null && !roadCode.isEmpty()) {
                roadFolderName = roadCode + "_" + roadFolderName;
            }
            
            String sectionFolderName = sanitizeFolderName(sectionName);
            if (sectionCode != null && !sectionCode.isEmpty()) {
                sectionFolderName = sectionCode + "_" + sectionFolderName;
            }
            
            Path sectionPath = Paths.get(basePath, roadFolderName, sectionFolderName);
            if (!Files.exists(sectionPath)) {
                Files.createDirectories(sectionPath);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("path", sectionPath.toAbsolutePath().toString());
            result.put("roadFolder", roadFolderName);
            result.put("sectionFolder", sectionFolderName);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "创建路段文件夹失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取道路文件夹路径")
    @GetMapping("/road/{roadId}/path")
    public Result<Map<String, Object>> getRoadPath(
            @PathVariable Long roadId,
            @RequestParam String roadName,
            @RequestParam(required = false) String roadCode) {
        try {
            String folderName = sanitizeFolderName(roadName);
            if (roadCode != null && !roadCode.isEmpty()) {
                folderName = roadCode + "_" + folderName;
            }
            
            Path roadPath = Paths.get(basePath, folderName);
            boolean exists = Files.exists(roadPath);
            
            Map<String, Object> result = new HashMap<>();
            result.put("path", roadPath.toAbsolutePath().toString());
            result.put("exists", exists);
            result.put("folderName", folderName);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "获取道路文件夹路径失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取路段文件夹路径")
    @GetMapping("/section/{sectionId}/path")
    public Result<Map<String, Object>> getSectionPath(
            @PathVariable Long sectionId,
            @RequestParam String roadName,
            @RequestParam String sectionName,
            @RequestParam(required = false) String roadCode,
            @RequestParam(required = false) String sectionCode) {
        try {
            String roadFolderName = sanitizeFolderName(roadName);
            if (roadCode != null && !roadCode.isEmpty()) {
                roadFolderName = roadCode + "_" + roadFolderName;
            }
            
            String sectionFolderName = sanitizeFolderName(sectionName);
            if (sectionCode != null && !sectionCode.isEmpty()) {
                sectionFolderName = sectionCode + "_" + sectionFolderName;
            }
            
            Path sectionPath = Paths.get(basePath, roadFolderName, sectionFolderName);
            boolean exists = Files.exists(sectionPath);
            
            Map<String, Object> result = new HashMap<>();
            result.put("path", sectionPath.toAbsolutePath().toString());
            result.put("exists", exists);
            result.put("roadFolder", roadFolderName);
            result.put("sectionFolder", sectionFolderName);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "获取路段文件夹路径失败: " + e.getMessage());
        }
    }

    @Operation(summary = "列出文件夹内容")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listFolder(@RequestParam String path) {
        try {
            Path folderPath = Paths.get(path);
            if (!Files.exists(folderPath)) {
                return Result.success(new ArrayList<>());
            }
            
            File folder = folderPath.toFile();
            File[] files = folder.listFiles();
            List<Map<String, Object>> result = new ArrayList<>();
            
            if (files != null) {
                for (File file : files) {
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("name", file.getName());
                    fileInfo.put("path", file.getAbsolutePath());
                    fileInfo.put("isDirectory", file.isDirectory());
                    fileInfo.put("size", file.length());
                    fileInfo.put("lastModified", new Date(file.lastModified()));
                    
                    if (!file.isDirectory()) {
                        String name = file.getName().toLowerCase();
                        fileInfo.put("isImage", name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".bmp"));
                        fileInfo.put("isVideo", name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mov") || name.endsWith(".mkv"));
                    }
                    
                    result.add(fileInfo);
                }
            }
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "列出文件夹内容失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取待检测文件列表")
    @GetMapping("/pending-files")
    public Result<List<Map<String, Object>>> getPendingFiles(
            @RequestParam String roadName,
            @RequestParam String sectionName,
            @RequestParam(required = false) String roadCode,
            @RequestParam(required = false) String sectionCode) {
        try {
            String roadFolderName = sanitizeFolderName(roadName);
            if (roadCode != null && !roadCode.isEmpty()) {
                roadFolderName = roadCode + "_" + roadFolderName;
            }
            
            String sectionFolderName = sanitizeFolderName(sectionName);
            if (sectionCode != null && !sectionCode.isEmpty()) {
                sectionFolderName = sectionCode + "_" + sectionFolderName;
            }
            
            Path sectionPath = Paths.get(basePath, roadFolderName, sectionFolderName);
            if (!Files.exists(sectionPath)) {
                return Result.success(new ArrayList<>());
            }
            
            List<Map<String, Object>> pendingFiles = new ArrayList<>();
            File folder = sectionPath.toFile();
            File[] files = folder.listFiles();
            
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        String name = file.getName().toLowerCase();
                        boolean isImage = name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".bmp");
                        boolean isVideo = name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mov") || name.endsWith(".mkv");
                        
                        if (isImage || isVideo) {
                            Map<String, Object> fileInfo = new HashMap<>();
                            fileInfo.put("name", file.getName());
                            fileInfo.put("path", file.getAbsolutePath());
                            fileInfo.put("type", isImage ? "image" : "video");
                            fileInfo.put("size", file.length());
                            fileInfo.put("lastModified", new Date(file.lastModified()));
                            pendingFiles.add(fileInfo);
                        }
                    }
                }
            }
            
            return Result.success(pendingFiles);
        } catch (Exception e) {
            return Result.error(500, "获取待检测文件列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "打开文件夹")
    @PostMapping("/open-folder")
    public Result<Void> openFolder(
            @RequestParam(required = false) String path,
            @RequestParam(required = false) Long roadId,
            @RequestParam(required = false) String roadName,
            @RequestParam(required = false) String roadCode,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) String sectionName,
            @RequestParam(required = false) String sectionCode) {
        try {
            Path folderPath;
            
            if (path != null && !path.isEmpty()) {
                folderPath = Paths.get(path);
            } else if (roadName != null && !roadName.isEmpty()) {
                String roadFolderName = sanitizeFolderName(
                    (roadCode != null && !roadCode.isEmpty() ? roadCode : roadId) + "_" + roadName
                );
                
                if (sectionName != null && !sectionName.isEmpty()) {
                    String sectionFolderName = sanitizeFolderName(
                        (sectionCode != null && !sectionCode.isEmpty() ? sectionCode : sectionId) + "_" + sectionName
                    );
                    folderPath = Paths.get(basePath, roadFolderName, sectionFolderName);
                } else {
                    folderPath = Paths.get(basePath, roadFolderName);
                }
            } else {
                return Result.error(400, "请提供路径或道路信息");
            }
            
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
            }
            
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            
            if (os.contains("win")) {
                pb = new ProcessBuilder("explorer", folderPath.toAbsolutePath().toString());
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("open", folderPath.toAbsolutePath().toString());
            } else {
                pb = new ProcessBuilder("xdg-open", folderPath.toAbsolutePath().toString());
            }
            
            pb.start();
            return Result.success();
        } catch (Exception e) {
            return Result.error(500, "打开文件夹失败: " + e.getMessage());
        }
    }

    private String sanitizeFolderName(String name) {
        if (name == null) return "unnamed";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    @Operation(summary = "列出视频文件")
    @GetMapping("/list-videos")
    public Result<Map<String, Object>> listVideos(@RequestParam String path) {
        try {
            Path folderPath;
            
            if (path == null || path.isEmpty()) {
                return Result.error(400, "路径不能为空");
            }
            
            if (Paths.get(path).isAbsolute()) {
                folderPath = Paths.get(path);
            } else {
                folderPath = Paths.get(basePath, path);
            }
            
            if (!Files.exists(folderPath)) {
                Map<String, Object> emptyResult = new HashMap<>();
                emptyResult.put("files", new ArrayList<>());
                emptyResult.put("path", path);
                return Result.success(emptyResult);
            }
            
            List<Map<String, Object>> videoFiles = new ArrayList<>();
            File folder = folderPath.toFile();
            File[] files = folder.listFiles();
            
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        String name = file.getName().toLowerCase();
                        boolean isVideo = name.endsWith(".mp4") || name.endsWith(".avi") || 
                                         name.endsWith(".mov") || name.endsWith(".mkv") ||
                                         name.endsWith(".webm") || name.endsWith(".wmv");
                        
                        if (isVideo) {
                            Map<String, Object> fileInfo = new HashMap<>();
                            fileInfo.put("name", file.getName());
                            fileInfo.put("path", file.getAbsolutePath());
                            fileInfo.put("url", "/storage/videos?path=" + java.net.URLEncoder.encode(file.getAbsolutePath(), "UTF-8"));
                            fileInfo.put("size", file.length());
                            fileInfo.put("lastModified", new Date(file.lastModified()));
                            videoFiles.add(fileInfo);
                        }
                    }
                }
            }
            
            java.util.Collections.sort(videoFiles, (a, b) -> {
                String nameA = (String) a.get("name");
                String nameB = (String) b.get("name");
                return nameA.compareToIgnoreCase(nameB);
            });
            
            Map<String, Object> result = new HashMap<>();
            result.put("files", videoFiles);
            result.put("path", folderPath.toAbsolutePath().toString());
            result.put("total", videoFiles.size());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "列出视频文件失败: " + e.getMessage());
        }
    }

    @Operation(summary = "选择文件夹")
    @GetMapping("/select-folder")
    public Result<Map<String, Object>> selectFolder() {
        try {
            javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
            chooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("选择视频文件夹");
            
            int result = chooser.showOpenDialog(null);
            if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                File selectedFolder = chooser.getSelectedFile();
                Map<String, Object> response = new HashMap<>();
                response.put("path", selectedFolder.getAbsolutePath());
                return Result.success(response);
            } else {
                return Result.error(400, "未选择文件夹");
            }
        } catch (Exception e) {
            return Result.error(500, "选择文件夹失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取视频文件")
    @GetMapping("/videos")
    public ResponseEntity<Resource> getVideo(@RequestParam String path) {
        try {
            String decodedPath = URLDecoder.decode(path, "UTF-8");
            Path videoPath = Paths.get(decodedPath);
            
            if (!Files.exists(videoPath)) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(videoPath);
            String contentType = Files.probeContentType(videoPath);
            if (contentType == null) {
                contentType = "video/mp4";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + videoPath.getFileName().toString() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
